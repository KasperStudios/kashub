package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Command for playing sounds
 * Syntax:
 *   sound play <name> [volume] [pitch]
 *   sound stop
 *   sound list
 *   sound note <note> [instrument]
 */
public class SoundCommand implements Command {
    
    // Quick sound aliases
    private static final Map<String, SoundEvent> SOUND_ALIASES = new HashMap<>();
    
    static {
        // Notifications
        SOUND_ALIASES.put("ding", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
        SOUND_ALIASES.put("levelup", SoundEvents.ENTITY_PLAYER_LEVELUP);
        SOUND_ALIASES.put("success", SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value());
        SOUND_ALIASES.put("error", SoundEvents.BLOCK_NOTE_BLOCK_BASS.value());
        SOUND_ALIASES.put("warning", SoundEvents.BLOCK_NOTE_BLOCK_BELL.value());
        SOUND_ALIASES.put("click", SoundEvents.UI_BUTTON_CLICK.value());
        
        // Alerts
        SOUND_ALIASES.put("alert", SoundEvents.BLOCK_BELL_USE);
        SOUND_ALIASES.put("ping", SoundEvents.BLOCK_AMETHYST_BLOCK_HIT);
        SOUND_ALIASES.put("found", SoundEvents.ENTITY_VILLAGER_YES);
        SOUND_ALIASES.put("notfound", SoundEvents.ENTITY_VILLAGER_NO);
        
        // Combat
        SOUND_ALIASES.put("hit", SoundEvents.ENTITY_PLAYER_HURT);
        SOUND_ALIASES.put("attack", SoundEvents.ENTITY_PLAYER_ATTACK_STRONG);
        SOUND_ALIASES.put("crit", SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
        SOUND_ALIASES.put("shield", SoundEvents.ITEM_SHIELD_BLOCK);
        
        // Items
        SOUND_ALIASES.put("equip", SoundEvents.ITEM_ARMOR_EQUIP_IRON.value());
        SOUND_ALIASES.put("pickup", SoundEvents.ENTITY_ITEM_PICKUP);
        SOUND_ALIASES.put("drop", SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM);
        
        // Blocks
        SOUND_ALIASES.put("break", SoundEvents.BLOCK_STONE_BREAK);
        SOUND_ALIASES.put("place", SoundEvents.BLOCK_STONE_PLACE);
        SOUND_ALIASES.put("chest", SoundEvents.BLOCK_CHEST_OPEN);
        
        // Ambient
        SOUND_ALIASES.put("thunder", SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER);
        SOUND_ALIASES.put("explode", SoundEvents.ENTITY_GENERIC_EXPLODE.value());
        SOUND_ALIASES.put("firework", SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH);
        
        // Music/Notes
        SOUND_ALIASES.put("harp", SoundEvents.BLOCK_NOTE_BLOCK_HARP.value());
        SOUND_ALIASES.put("bass", SoundEvents.BLOCK_NOTE_BLOCK_BASS.value());
        SOUND_ALIASES.put("bell", SoundEvents.BLOCK_NOTE_BLOCK_BELL.value());
        SOUND_ALIASES.put("chime", SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value());
        SOUND_ALIASES.put("flute", SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value());
        SOUND_ALIASES.put("guitar", SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value());
        SOUND_ALIASES.put("xylophone", SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value());
        SOUND_ALIASES.put("iron_xylophone", SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value());
        SOUND_ALIASES.put("cow_bell", SoundEvents.BLOCK_NOTE_BLOCK_COW_BELL.value());
        SOUND_ALIASES.put("didgeridoo", SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value());
        SOUND_ALIASES.put("bit", SoundEvents.BLOCK_NOTE_BLOCK_BIT.value());
        SOUND_ALIASES.put("banjo", SoundEvents.BLOCK_NOTE_BLOCK_BANJO.value());
        SOUND_ALIASES.put("pling", SoundEvents.BLOCK_NOTE_BLOCK_PLING.value());
    }
    
    @Override
    public String getName() {
        return "sound";
    }
    
    @Override
    public String getDescription() {
        return "Play sounds for notifications and alerts";
    }
    
    @Override
    public String getParameters() {
        return "<play|stop|list|note|broadcast> [name] [volume] [pitch]";
    }

    @Override
    public String getCategory() {
        return "Sound";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Play sounds and music.\n\n" +
               "Usage:\n" +
               "  sound play <name> [volume] [pitch]\n" +
               "  sound broadcast <name> [volume] [pitch]\n" +
               "  sound note <0-24> [instrument]\n" +
               "  sound music <notes> [instrument] [tempo]\n\n" +
               "Sound aliases:\n" +
               "  ding, levelup, success, error, warning\n" +
               "  alert, ping, found, notfound, hit\n\n" +
               "Instruments:\n" +
               "  harp, bass, bell, chime, flute, guitar\n" +
               "  xylophone, iron_xylophone, cow_bell\n" +
               "  didgeridoo, bit, banjo, pling\n\n" +
               "Music example:\n" +
               "  sound music C4,D4,E4,F4,G4 harp 200";
    }
    
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "play":
                if (args.length < 2) {
                    System.out.println("Usage: sound play <name> [volume] [pitch]");
                    return;
                }
                
                String soundName = args[1].toLowerCase();
                float volume = args.length > 2 ? Float.parseFloat(args[2]) : 1.0f;
                float pitch = args.length > 3 ? Float.parseFloat(args[3]) : 1.0f;
                
                playSound(client, soundName, volume, pitch);
                break;
                
            case "stop":
                client.execute(() -> client.getSoundManager().stopAll());
                break;
                
            case "list":
                System.out.println("Available sound aliases:");
                SOUND_ALIASES.keySet().stream().sorted().forEach(name -> 
                    System.out.println("  - " + name));
                break;
                
            case "note":
                // Play a musical note (local only)
                if (args.length < 2) {
                    System.out.println("Usage: sound note <0-24> [instrument]");
                    return;
                }
                
                int note = Integer.parseInt(args[1]);
                String instrument = args.length > 2 ? args[2].toLowerCase() : "harp";
                float notePitch = (float) Math.pow(2.0, (note - 12) / 12.0);
                
                SoundEvent noteSound = SOUND_ALIASES.getOrDefault(instrument, SoundEvents.BLOCK_NOTE_BLOCK_HARP.value());
                client.execute(() -> {
                    client.getSoundManager().play(PositionedSoundInstance.master(noteSound, notePitch, 1.0f));
                });
                break;
                
            case "broadcast":
                // Play sound at player position (other players can hear)
                if (args.length < 2) {
                    System.out.println("Usage: sound broadcast <name> [volume] [pitch]");
                    return;
                }
                
                String bcastName = args[1].toLowerCase();
                float bcastVol = args.length > 2 ? Float.parseFloat(args[2]) : 1.0f;
                float bcastPitch = args.length > 3 ? Float.parseFloat(args[3]) : 1.0f;
                
                playSoundAtPlayer(client, bcastName, bcastVol, bcastPitch);
                break;
                
            case "music":
                // Play a sequence of notes (simple melody)
                if (args.length < 2) {
                    System.out.println("Usage: sound music <notes> [instrument] [tempo]");
                    System.out.println("Notes format: C4,D4,E4,F4 or 0,2,4,5 (semitones)");
                    return;
                }
                
                String notesStr = args[1];
                String musicInstrument = args.length > 2 ? args[2].toLowerCase() : "harp";
                int tempo = args.length > 3 ? Integer.parseInt(args[3]) : 200; // ms per note
                
                playMelody(client, notesStr, musicInstrument, tempo);
                break;
                
            default:
                // Try to play directly
                playSound(client, subcommand, 1.0f, 1.0f);
        }
    }
    
    private void playSound(MinecraftClient client, String soundName, float volume, float pitch) {
        client.execute(() -> {
            SoundEvent sound = null;
            
            // Check aliases first
            if (SOUND_ALIASES.containsKey(soundName)) {
                sound = SOUND_ALIASES.get(soundName);
            } else {
                // Try to find by identifier
                try {
                    Identifier id = Identifier.of(soundName.contains(":") ? soundName : "minecraft:" + soundName);
                    sound = Registries.SOUND_EVENT.get(id);
                } catch (Exception e) {
                    System.out.println("Sound not found: " + soundName);
                    return;
                }
            }
            
            if (sound != null) {
                client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
            }
        });
    }
    
    private void playSoundAtPlayer(MinecraftClient client, String soundName, float volume, float pitch) {
        client.execute(() -> {
            if (client.player == null || client.world == null) return;
            
            SoundEvent sound = SOUND_ALIASES.get(soundName);
            if (sound == null) {
                try {
                    Identifier id = Identifier.of(soundName.contains(":") ? soundName : "minecraft:" + soundName);
                    sound = Registries.SOUND_EVENT.get(id);
                } catch (Exception e) {
                    return;
                }
            }
            
            if (sound != null) {
                // Play at player position - other players nearby can hear
                client.world.playSound(
                    client.player,
                    client.player.getBlockPos(),
                    sound,
                    SoundCategory.PLAYERS,
                    volume,
                    pitch
                );
            }
        });
    }
    
    private void playMelody(MinecraftClient client, String notesStr, String instrument, int tempo) {
        // Parse notes: can be "C4,D4,E4" or "0,2,4,5" (semitones from C4)
        String[] noteNames = notesStr.split(",");
        SoundEvent sound = SOUND_ALIASES.getOrDefault(instrument, SoundEvents.BLOCK_NOTE_BLOCK_HARP.value());
        
        // Play in separate thread
        new Thread(() -> {
            for (String noteName : noteNames) {
                if (state_stopped) break;
                
                float pitch = parseNoteToPitch(noteName.trim());
                
                final SoundEvent finalSound = sound;
                final float finalPitch = pitch;
                client.execute(() -> {
                    client.getSoundManager().play(PositionedSoundInstance.master(finalSound, finalPitch, 1.0f));
                });
                
                try {
                    Thread.sleep(tempo);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    private static volatile boolean state_stopped = false;
    
    private float parseNoteToPitch(String note) {
        // Try as number first (semitones from middle C)
        try {
            int semitone = Integer.parseInt(note);
            return (float) Math.pow(2.0, (semitone - 12) / 12.0);
        } catch (NumberFormatException e) {
            // Parse as note name (C4, D#4, etc.)
        }
        
        // Note name parsing
        note = note.toUpperCase();
        int semitone = 0;
        
        char baseNote = note.charAt(0);
        switch (baseNote) {
            case 'C': semitone = 0; break;
            case 'D': semitone = 2; break;
            case 'E': semitone = 4; break;
            case 'F': semitone = 5; break;
            case 'G': semitone = 7; break;
            case 'A': semitone = 9; break;
            case 'B': semitone = 11; break;
            default: return 1.0f;
        }
        
        int idx = 1;
        // Check for sharp/flat
        if (note.length() > 1) {
            if (note.charAt(1) == '#') {
                semitone++;
                idx++;
            } else if (note.charAt(1) == 'B') {
                semitone--;
                idx++;
            }
        }
        
        // Get octave (default 4)
        int octave = 4;
        if (idx < note.length()) {
            try {
                octave = Integer.parseInt(note.substring(idx));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        
        // Calculate pitch relative to C4 (middle C)
        int totalSemitones = semitone + (octave - 4) * 12;
        return (float) Math.pow(2.0, (totalSemitones) / 12.0);
    }
    
    private void printHelp() {
        System.out.println("Sound commands:");
        System.out.println("  sound play <name> [volume] [pitch] - Play a sound (local)");
        System.out.println("  sound broadcast <name> [volume] [pitch] - Play at player pos (others hear)");
        System.out.println("  sound stop - Stop all sounds");
        System.out.println("  sound list - List available sound aliases");
        System.out.println("  sound note <0-24> [instrument] - Play a musical note");
        System.out.println("  sound music <notes> [instrument] [tempo] - Play melody");
        System.out.println("");
        System.out.println("Notes format: C4,D4,E4,F4 or 0,2,4,5,7 (semitones)");
        System.out.println("Instruments: harp, bass, bell, chime, flute, guitar, xylophone, pling");
        System.out.println("Quick aliases: ding, levelup, success, error, warning, alert, ping, found");
    }
}
