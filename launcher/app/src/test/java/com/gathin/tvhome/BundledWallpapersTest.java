package com.gathin.tvhome;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import static org.junit.Assert.*;

public class BundledWallpapersTest {
    @Test public void createsParentAndCopiesBytesExactly() throws Exception {
        File root = Files.createTempDirectory("wallpaper-test-").toFile();
        File target = new File(new File(root, "wallpapers"), "image.jpg");
        byte[] bytes = new byte[40_000];
        Arrays.fill(bytes, (byte) 127);
        try {
            assertTrue(BundledWallpapers.copyMissing(new ByteArrayInputStream(bytes), target));
            assertArrayEquals(bytes, Files.readAllBytes(target.toPath()));
            assertEquals(1, target.getParentFile().list().length);
        } finally { clean(root); }
    }
    @Test public void upgradeDoesNotOverwriteAnExistingPicture() throws Exception {
        File root = Files.createTempDirectory("wallpaper-test-").toFile();
        File target = new File(root, "user.jpg");
        byte[] existing = {1, 2, 3};
        Files.write(target.toPath(), existing);
        try {
            assertFalse(BundledWallpapers.copyMissing(new ByteArrayInputStream(new byte[]{9}), target));
            assertArrayEquals(existing, Files.readAllBytes(target.toPath()));
        } finally { clean(root); }
    }
    @Test public void failedReadNeverPublishesAPartialPicture() throws Exception {
        File root = Files.createTempDirectory("wallpaper-test-").toFile();
        File target = new File(root, "missing.jpg");
        InputStream broken = new InputStream() {
            int calls;
            @Override public int read() throws IOException { throw new IOException("unavailable"); }
            @Override public int read(byte[] b, int offset, int length) throws IOException {
                if (calls++ == 0) { b[offset] = 1; return 1; }
                throw new IOException("interrupted asset");
            }
        };
        try {
            try { BundledWallpapers.copyMissing(broken, target); fail("Expected read failure"); }
            catch (IOException expected) { }
            assertFalse(target.exists());
            assertEquals(0, root.list().length);
        } finally { clean(root); }
    }
    @Test public void rejectsAFileWhereTheDirectoryShouldBe() throws Exception {
        File root = Files.createTempDirectory("wallpaper-test-").toFile();
        File blocker = new File(root, "not-a-folder");
        Files.write(blocker.toPath(), new byte[]{4});
        try {
            try { BundledWallpapers.copyMissing(new ByteArrayInputStream(new byte[]{1}), new File(blocker, "x.jpg")); fail("Expected directory failure"); }
            catch (IOException expected) { }
            assertArrayEquals(new byte[]{4}, Files.readAllBytes(blocker.toPath()));
        } finally { clean(root); }
    }
    private static void clean(File file) throws IOException {
        if (file.isDirectory()) for (File child : file.listFiles()) clean(child);
        Files.deleteIfExists(file.toPath());
    }
}
