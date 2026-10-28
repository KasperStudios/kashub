package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.util.VirtualFileSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VfsObject - DSL interface for the VirtualFileSystem.
 */
public class VfsObject extends Value implements Documentable {

    public VfsObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("list", Value.of((Callable) (ctx, args) -> {
            String path = args.isEmpty() ? "~/" : args.get(0).asString();
            // Get current directory from TerminalManager directly
            String curDir = kasperstudios.kashub.core.TerminalManager.getInstance().getCurrentDir();
            
            List<VirtualFileSystem.VfsNode> nodes = VirtualFileSystem.getInstance().list(path, curDir);
            List<Value> result = new ArrayList<>();
            for (VirtualFileSystem.VfsNode node : nodes) {
                Map<String, Value> nodeMap = new HashMap<>();
                nodeMap.put("name", Value.of(node.name));
                nodeMap.put("isDirectory", Value.of(node.isDirectory));
                nodeMap.put("virtualPath", Value.of(node.virtualPath));
                result.add(Value.of(nodeMap));
            }
            return Value.of(result);
        }));

        methods.put("exists", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            String path = args.get(0).asString();
            String curDir = kasperstudios.kashub.core.TerminalManager.getInstance().getCurrentDir();
            java.nio.file.Path p = VirtualFileSystem.getInstance().resolve(path, curDir);
            return Value.of(p != null && java.nio.file.Files.exists(p));
        }));

        methods.put("isDirectory", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            String path = args.get(0).asString();
            String curDir = kasperstudios.kashub.core.TerminalManager.getInstance().getCurrentDir();
            java.nio.file.Path p = VirtualFileSystem.getInstance().resolve(path, curDir);
            return Value.of(p != null && java.nio.file.Files.isDirectory(p));
        }));

        return methods;
    }

    @Override
    public String getObjectName() {
        return "VFS";
    }

    @Override
    public String getObjectDescription() {
        return "DSL interface for interacting with the Virtual File System.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("list([path])", "Returns a list of nodes (files/directories) in the specified path or current directory.");
        docs.put("exists(path)", "Returns true if the specified virtual path exists.");
        docs.put("isDirectory(path)", "Returns true if the specified virtual path is a directory.");
        return docs;
    }
}
