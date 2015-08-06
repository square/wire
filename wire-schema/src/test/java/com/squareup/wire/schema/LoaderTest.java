package com.squareup.wire.schema;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by edwin.vane on 2015-08-07.
 */
public class LoaderTest {
    @Rule
    public final TemporaryFolder tempFolder1 = new TemporaryFolder();

    @Rule
    public final TemporaryFolder tempFolder2 = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void locateInMultiplePaths() throws IOException {
        File file1 = tempFolder1.newFile();
        File file2 = tempFolder2.newFile();

        Loader loader = Loader.forSearchPaths(Arrays.asList(tempFolder1.getRoot().getPath(), tempFolder2.getRoot().getPath()));
        loader.load(Arrays.asList(file1.getName(), file2.getName()));
    }

    @Test
    public void failLocate() throws IOException {
        File file = tempFolder2.newFile();

        Loader loader = Loader.forSearchPaths(Arrays.asList(tempFolder1.getRoot().getPath()));

        exception.expect(IOException.class);
        loader.load(Arrays.asList(file.getName()));
    }
}
