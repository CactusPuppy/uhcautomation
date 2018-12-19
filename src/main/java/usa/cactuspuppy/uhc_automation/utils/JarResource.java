package usa.cactuspuppy.uhc_automation.utils;

import org.apache.commons.io.FileUtils;
import usa.cactuspuppy.uhc_automation.Main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class JarResource {
    /**
     * Extracts a jar resource to the specified relative output path.
     * @param inputStream inputStream to copy
     * @param relativeOutput file path to extract to
     * @param overwrite whether to overwrite the output file if it exists
     * @return status code for success or failure
     * -1 - Illegal args
     * 0 - OK/Success
     * 1 - I/O Problem
     * 2 - Output file exists (overwrite false only)
     */
    public static int extractResource(InputStream inputStream, String relativeOutput, boolean overwrite) {
        if (inputStream == null || relativeOutput == null) return -1;
        File outFile = new File(relativeOutput);
        if (!overwrite && outFile.isFile()) return 2;
        try {
            FileUtils.copyInputStreamToFile(inputStream, outFile);
        } catch (IOException e) {
            Logger.logError(JarResource.class, "Could not write resource to specified outfile", Optional.of(e));
            return 1;
        }
        return 0;
    }
}