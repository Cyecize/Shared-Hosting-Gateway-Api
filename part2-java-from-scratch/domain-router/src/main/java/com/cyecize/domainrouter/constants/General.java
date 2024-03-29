package com.cyecize.domainrouter.constants;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class General {
    private final static String START_UP_PACKAGE_PATH = General.class.getName()
            .replace(General.class.getSimpleName(), "")
            .replaceAll("\\.", "/");

    public static final String WORKING_DIRECTORY = URLDecoder.decode(General.class.getResource("").toString()
            .replace("file:/", "/")
            .replace(START_UP_PACKAGE_PATH, ""), StandardCharsets.UTF_8);

    public static final String SETTINGS_FILE_NAME = "options.json";

    public static final String ENV_VAR_PORT_NAME = "port";

    public static final String ENV_VAR_OPTIONS_NAME = "options";

    public static final String ENV_VAR_POOL_SIZE_NAME = "poolSize";

    public static final int READ_BUFFER_SIZE = 8196;

    public static final int DEFAULT_PORT = 80;

    public static final int DEFAULT_THREAD_POOL_SIZE = 20;

    public static final int MIN_THREAD_POOL_SIZE = 3;
}
