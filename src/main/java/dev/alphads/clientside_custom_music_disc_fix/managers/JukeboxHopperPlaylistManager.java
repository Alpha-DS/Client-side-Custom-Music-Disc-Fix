package dev.alphads.clientside_custom_music_disc_fix.managers;

import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.LinkedList;

/** This class manages and stores the playlist of songs if the simulate jukebox hopper config is enabled. */

public class JukeboxHopperPlaylistManager {
    private static final HashMap<BlockPos, Playlist> playlistHashMap = new HashMap<>();

    public static void addSongToPlaylist(BlockPos jukeboxPos, RegistryEntry<JukeboxSong> song) {
        if (playlistHashMap.containsKey(jukeboxPos)) {
            playlistHashMap.get(jukeboxPos).songQueue.add(song);
        } else {
            playlistHashMap.put(jukeboxPos, new Playlist(song));
        }
    }

    public static RegistryEntry<JukeboxSong> getSongFromPlaylist(BlockPos jukeboxPos) {
        if (playlistHashMap.containsKey(jukeboxPos)) {
            Playlist playlist = playlistHashMap.get(jukeboxPos);
            RegistryEntry<JukeboxSong> song = playlist.songQueue.poll();
            if (playlist.songQueue.isEmpty()) {
                playlistHashMap.remove(jukeboxPos);
            }
            return song;
        }
        return null;
    }

    public static void removePlaylist(BlockPos jukeboxPos) {
        playlistHashMap.remove(jukeboxPos);
    }

    private static class Playlist {
        private final LinkedList<RegistryEntry<JukeboxSong>> songQueue = new LinkedList<>();

        public Playlist(RegistryEntry<JukeboxSong> song) {
            songQueue.add(song);
        }
    }
}
