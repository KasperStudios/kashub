import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListToolsRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import fs from "fs";
import path from "path";
import { WebSocket } from "ws";

import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PROJECT_ROOT = process.env.KASHUB_PROJECT_ROOT || path.resolve(__dirname, "..");
const LOG_FILE = process.env.KASHUB_LOG_FILE || path.join(PROJECT_ROOT, "run", "logs", "latest.log");
const WS_URL = process.env.KASHUB_WS_URL || "ws://localhost:25567";

const server = new Server(
  {
    name: "kashub-mcp",
    version: "1.2.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "list_scripts",
        description: "List all .kh scripts in the project",
        inputSchema: { type: "object", properties: {} },
      },
      {
        name: "get_logs",
        description: "Read the last N lines of Minecraft logs",
        inputSchema: {
          type: "object",
          properties: {
            lines: { type: "number", description: "Number of lines to read", default: 50 },
          },
        },
      },
      {
        name: "execute_kashub_code",
        description: "Execute raw KasHub code via WebSocket",
        inputSchema: {
          type: "object",
          properties: {
            code: { type: "string", description: "KasHub code to execute" },
          },
          required: ["code"],
        },
      },
      {
        name: "scan_target_peripheral",
        description: "Scans the peripheral the player is looking at and returns its methods",
        inputSchema: { type: "object", properties: {} },
      },
    ],
  };
});

async function sendWsCommand(command) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(WS_URL);
    let timeout = setTimeout(() => {
      ws.close();
      reject(new Error("WebSocket timeout"));
    }, 5000);

    ws.on("open", () => {
      ws.send(JSON.stringify(command));
      if (command.type === "launch" || command.type === "stop_all") {
        // Wait for response
      } else {
        clearTimeout(timeout);
        ws.close();
        resolve("Command sent");
      }
    });

    ws.on("message", (data) => {
      clearTimeout(timeout);
      ws.close();
      resolve(JSON.parse(data.toString()));
    });

    ws.on("error", (err) => {
      clearTimeout(timeout);
      reject(err);
    });
  });
}

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "list_scripts": {
        const files = fs.readdirSync(PROJECT_ROOT).filter(f => f.endsWith(".kh"));
        return { content: [{ type: "text", text: files.join("\n") }] };
      }
      case "get_logs": {
        const linesToRead = args.lines || 50;
        if (!fs.existsSync(LOG_FILE)) return { content: [{ type: "text", text: "Log file not found." }] };
        const content = fs.readFileSync(LOG_FILE, "utf-8");
        const lines = content.split("\n");
        const lastLines = lines.slice(-linesToRead).join("\n");
        return { content: [{ type: "text", text: lastLines }] };
      }
      case "execute_kashub_code": {
        const response = await sendWsCommand({
          type: "launch",
          program: "mcp_exec",
          code: args.code
        });
        return { content: [{ type: "text", text: JSON.stringify(response, null, 2) }] };
      }
      case "scan_target_peripheral": {
        const response = await sendWsCommand({
          type: "debug_peripheral"
        });
        return { content: [{ type: "text", text: JSON.stringify(response, null, 2) }] };
      }
      default:
        throw new Error(`Unknown tool: ${name}`);
    }
  } catch (error) {
    return {
      isError: true,
      content: [{ type: "text", text: error.message }],
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("KasHub MCP Server v1.2.0 running on stdio");
}

main().catch((error) => {
  console.error("Fatal error in main():", error);
  process.exit(1);
});
