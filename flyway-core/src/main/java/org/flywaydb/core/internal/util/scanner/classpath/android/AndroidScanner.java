/*
 * Copyright 2010-2018 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.util.scanner.classpath.android;

import android.content.Context;
import dalvik.system.DexFile;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.android.ContextHolder;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.util.ClassUtils;
import org.flywaydb.core.internal.util.Location;
import org.flywaydb.core.internal.util.scanner.LoadableResource;
import org.flywaydb.core.internal.util.scanner.classpath.ResourceAndClassScanner;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Class & resource scanner for Android.
 */
public class AndroidScanner implements ResourceAndClassScanner {
    private static final Log LOG = LogFactory.getLog(AndroidScanner.class);

    private final Context context;

    private final ClassLoader classLoader;

    public AndroidScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
        context = ContextHolder.getContext();
        if (context == null) {
            throw new FlywayException("Unable to scan for Migrations! Context not set. " +
                    "Within an activity you can fix this with org.flywaydb.core.api.android.ContextHolder.setContext(this);");
        }
    }

    public LoadableResource[] scanForResources(Location location, String prefix, String[] suffixes) throws Exception {
        List<LoadableResource> resources = new ArrayList<>();

        String path = location.getPath();
        for (String asset : context.getAssets().list(path)) {
            if (assetMatches(asset, prefix, suffixes)) {
                resources.add(new AndroidResource(context.getAssets(), path, asset));
            } else {
                LOG.debug("Filtering out asset: " + asset);
            }
        }

        return resources.toArray(new LoadableResource[resources.size()]);
    }

    private boolean assetMatches(String asset, String prefix, String[] suffixes) {
        for (String suffix : suffixes) {
            if (asset.startsWith(prefix) && asset.endsWith(suffix)
                    && (asset.length() > (prefix + suffix).length())) {
                return true;
            }
        }
        return false;
    }

    public Class<?>[] scanForClasses(Location location, Class<?> implementedInterface) throws Exception {
        String pkg = location.getPath().replace("/", ".");

        List<Class> classes = new ArrayList<>();
        DexFile dex = null;
        try {
            dex = new DexFile(context.getApplicationInfo().sourceDir);
            Enumeration<String> entries = dex.entries();
            while (entries.hasMoreElements()) {
                String className = entries.nextElement();
                if (className.startsWith(pkg)) {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (Modifier.isAbstract(clazz.getModifiers())) {
                        LOG.debug("Skipping abstract class: " + className);
                        continue;
                    }

                    if (!implementedInterface.isAssignableFrom(clazz)) {
                        continue;
                    }

                    ClassUtils.instantiate(className, classLoader);

                    classes.add(clazz);
                    LOG.debug("Found class: " + className);
                }
            }
        } finally {
            if (dex != null) {
                dex.close();
            }
        }
        return classes.toArray(new Class<?>[classes.size()]);
    }
}