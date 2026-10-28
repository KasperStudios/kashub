package kasperstudios.kashub.services.integration;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages static documentation and snippets for CC:T peripherals.
 * Used by the Peripheral Scan feature to provide usage examples.
 */
public class PeripheralDocsManager {
    
    public record MethodDoc(String description, String snippet) {}

    private static final Map<String, Map<String, MethodDoc>> PERIPHERAL_DOCS = new HashMap<>();

    static {
        // ---- INVENTORY / CHEST ----
        Map<String, MethodDoc> inventory = new HashMap<>();
        inventory.put("size", new MethodDoc(
            "Get the number of slots in the inventory.",
            "local size = peripheral.call(\"top\", \"size\")\nprint(\"Slots: \" .. size)"
        ));
        inventory.put("list", new MethodDoc(
            "Get a list of all items in the inventory.",
            "local items = peripheral.call(\"top\", \"list\")\nfor slot, item in pairs(items) do\n  print(slot .. \": \" .. item.name .. \" x\" .. item.count)\nend"
        ));
        inventory.put("getItemDetail", new MethodDoc(
            "Get detailed information about an item in a specific slot.",
            "local detail = peripheral.call(\"top\", \"getItemDetail\", 1)\nif detail then\n  print(\"Item: \" .. detail.name)\nend"
        ));
        inventory.put("pushItems", new MethodDoc(
            "Push items to another inventory.",
            "-- Push 64 items from slot 1 to the 'left' inventory\nperipheral.call(\"top\", \"pushItems\", \"left\", 1, 64)"
        ));
        inventory.put("pullItems", new MethodDoc(
            "Pull items from another inventory.",
            "-- Pull 64 items from slot 1 of the 'left' inventory\nperipheral.call(\"top\", \"pullItems\", \"left\", 1, 64)"
        ));
        PERIPHERAL_DOCS.put("inventory", inventory);
        PERIPHERAL_DOCS.put("minecraft:chest", inventory);
        PERIPHERAL_DOCS.put("minecraft:barrel", inventory);

        // ---- MONITOR ----
        Map<String, MethodDoc> monitor = new HashMap<>();
        monitor.put("write", new MethodDoc(
            "Write text to the monitor.",
            "local m = peripheral.wrap(\"top\")\nm.write(\"Hello World!\")"
        ));
        monitor.put("clear", new MethodDoc(
            "Clear the monitor screen.",
            "local m = peripheral.wrap(\"top\")\nm.clear()\nm.setCursorPos(1, 1)"
        ));
        monitor.put("setCursorPos", new MethodDoc(
            "Set the cursor position.",
            "local m = peripheral.wrap(\"top\")\nm.setCursorPos(5, 5)\nm.write(\"Center-ish\")"
        ));
        monitor.put("setTextScale", new MethodDoc(
            "Set the text scale (0.5 to 5.0).",
            "local m = peripheral.wrap(\"top\")\nm.setTextScale(2.0)\nm.write(\"BIG TEXT\")"
        ));
        monitor.put("setTextColor", new MethodDoc(
            "Set the text color.",
            "local m = peripheral.wrap(\"top\")\nm.setTextColor(colors.red)\nm.write(\"Error!\")"
        ));
        monitor.put("setBackgroundColor", new MethodDoc(
            "Set the background color.",
            "local m = peripheral.wrap(\"top\")\nm.setBackgroundColor(colors.blue)\nm.clear()"
        ));
        PERIPHERAL_DOCS.put("monitor", monitor);

        // ---- DRIVE ----
        Map<String, MethodDoc> drive = new HashMap<>();
        drive.put("isDiskPresent", new MethodDoc(
            "Check if a floppy disk or record is inserted.",
            "if peripheral.call(\"top\", \"isDiskPresent\") then\n  print(\"Disk found!\")\nend"
        ));
        drive.put("getDiskLabel", new MethodDoc(
            "Get the label of the inserted disk.",
            "local label = peripheral.call(\"top\", \"getDiskLabel\")\nprint(\"Label: \" .. tostring(label))"
        ));
        drive.put("getMountPath", new MethodDoc(
            "Get the path where the disk is mounted.",
            "local path = peripheral.call(\"top\", \"getMountPath\")\nprint(\"Mounted at: /\" .. path)"
        ));
        drive.put("ejectDisk", new MethodDoc(
            "Eject the inserted disk.",
            "peripheral.call(\"top\", \"ejectDisk\")"
        ));
        drive.put("playAudio", new MethodDoc(
            "Play the inserted music disc.",
            "peripheral.call(\"top\", \"playAudio\")"
        ));
        PERIPHERAL_DOCS.put("drive", drive);
        
        // ---- SPEAKER ----
        Map<String, MethodDoc> speaker = new HashMap<>();
        speaker.put("playSound", new MethodDoc(
            "Play a Minecraft sound event.",
            "local speaker = peripheral.wrap(\"top\")\nspeaker.playSound(\"entity.experience_orb.pickup\", 1.0, 1.0)"
        ));
        speaker.put("playNote", new MethodDoc(
            "Play an instrument note.",
            "local speaker = peripheral.wrap(\"top\")\n-- Instrument name, volume, pitch (0-24)\nspeaker.playNote(\"harp\", 1.0, 12)"
        ));
        PERIPHERAL_DOCS.put("speaker", speaker);

        // ---- PRINTER ----
        Map<String, MethodDoc> printer = new HashMap<>();
        printer.put("newPage", new MethodDoc(
            "Start printing a new page. Requires paper and ink.",
            "local p = peripheral.wrap(\"top\")\nif p.newPage() then\n  p.write(\"Hello Printed World\")\n  p.endPage()\nend"
        ));
        printer.put("endPage", new MethodDoc(
            "Output the printed page.",
            "local p = peripheral.wrap(\"top\")\np.endPage()"
        ));
        printer.put("getInkLevel", new MethodDoc(
            "Get the remaining ink level.",
            "print(\"Ink: \" .. peripheral.call(\"top\", \"getInkLevel\"))"
        ));
        printer.put("getPaperLevel", new MethodDoc(
            "Get the remaining paper level.",
            "print(\"Paper: \" .. peripheral.call(\"top\", \"getPaperLevel\"))"
        ));
        PERIPHERAL_DOCS.put("printer", printer);
        
        // ---- CREATE: MOTOR (Example mod support) ----
        Map<String, MethodDoc> createMotor = new HashMap<>();
        createMotor.put("getSpeed", new MethodDoc(
            "Get the current speed of the motor.",
            "print(\"Speed: \" .. peripheral.call(\"top\", \"getSpeed\"))"
        ));
        createMotor.put("setSpeed", new MethodDoc(
            "Set the speed of the motor.",
            "peripheral.call(\"top\", \"setSpeed\", 128)"
        ));
        PERIPHERAL_DOCS.put("create:motor", createMotor);
        PERIPHERAL_DOCS.put("electric_motor", createMotor); // typical name in Create Crafts & Additions
    }

    public static MethodDoc getDoc(String peripheralType, String methodName) {
        String cleanMethod = methodName;
        if (methodName.endsWith("()")) {
            cleanMethod = methodName.substring(0, methodName.length() - 2);
        }
        
        Map<String, MethodDoc> methods = PERIPHERAL_DOCS.get(peripheralType.toLowerCase());
        if (methods != null) {
            return methods.get(cleanMethod);
        }
        return null;
    }
}
