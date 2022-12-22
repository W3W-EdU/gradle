/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.util

import org.gradle.api.JavaVersion
import org.gradle.internal.os.OperatingSystem
import org.testcontainers.DockerClientFactory

import static org.gradle.util.TestPrecondition.doSatisfies
import static org.gradle.util.TestPrecondition.notSatisfies

class UnitTestPreconditions {

    static final class Symlinks implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return doSatisfies(MacOs) || doSatisfies(Linux)
        }
    }

    static final class NoSymlinks implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(Symlinks)
        }
    }

    static final class CaseInsensitiveFs implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return doSatisfies(MacOs) || doSatisfies(Windows)
        }
    }

    static final class FilePermissions implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return doSatisfies(MacOs) || doSatisfies(Linux)
        }
    }

    static final class NoFilePermissions implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(FilePermissions)
        }
    }

    static final class WorkingDir implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() < JavaVersion.VERSION_11
        }
    }

    static final class MandatoryFileLockOnOpen implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return doSatisfies(Windows)
        }
    }

    static final class NoMandatoryFileLockOnOpen implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(MandatoryFileLockOnOpen)
        }
    }

    static final class Windows implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return OperatingSystem.current().isWindows()
        }
    }

    static final class NotWindows implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(Windows)
        }
    }

    static final class MacOs implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return OperatingSystem.current().isMacOsX()
        }
    }

    static final class NotMacOs implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(MacOs)
        }
    }

    static final class MacOsM1 implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return doSatisfies(MacOs) && OperatingSystem.current().toString().contains("aarch64")
        }
    }

    static final class NotMacOsM1 implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(MacOsM1)
        }
    }

    static final class Linux implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return OperatingSystem.current().linux
        }
    }

    static final class NotLinux implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(Linux)
        }
    }

    static final class Unix implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return OperatingSystem.current().isUnix()
        }
    }

    static final class UnixDerivative implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            doSatisfies(MacOs) || doSatisfies(Linux) || doSatisfies(Unix)
        }
    }

    static final class HasDocker implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            try {
                DockerClientFactory.instance().client()
            } catch (Exception ex) {
                return false
            }
            return true
        }
    }

    static final class Jdk6OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_1_6
        }
    }

    static final class Jdk6OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_1_6
        }
    }

    static final class Jdk7OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_1_7
        }
    }

    static final class Jdk7OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_1_7
        }
    }

    static final class Jdk8OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_1_8
        }
    }

    static final class Jdk8OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_1_8
        }
    }

    static final class Jdk9OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_1_9
        }
    }

    static final class Jdk9OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_1_9
        }
    }

    static final class Jdk10OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_1_10
        }
    }

    static final class Jdk10OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_1_10
        }
    }

    static final class Jdk11OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_11
        }
    }

    static final class Jdk11OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_11
        }
    }

    static final class Jdk12OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_12
        }
    }

    static final class Jdk12OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_12
        }
    }

    static final class Jdk13OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_13
        }
    }

    static final class Jdk13OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_13
        }
    }

    static final class Jdk14OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_14
        }
    }

    static final class Jdk14OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_14
        }
    }

    static final class Jdk15OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_15
        }
    }

    static final class Jdk15OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_15
        }
    }

    static final class Jdk16OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_16
        }
    }

    static final class Jdk16OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_16
        }
    }

    static final class Jdk17OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_17
        }
    }

    static final class Jdk17OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_17
        }
    }

    static final class Jdk18OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_18
        }
    }

    static final class Jdk18OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_18
        }
    }

    static final class Jdk19OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_19
        }
    }

    static final class Jdk19OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_19
        }
    }

    static final class Jdk20OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_20
        }
    }

    static final class Jdk20OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_20
        }
    }

    static final class Jdk21OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_21
        }
    }

    static final class Jdk21OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_21
        }
    }

    static final class Jdk22OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_22
        }
    }

    static final class Jdk22OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_22
        }
    }

    static final class Jdk23OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_23
        }
    }

    static final class Jdk23OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_23
        }
    }

    static final class Jdk24OrLater implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() >= JavaVersion.VERSION_24
        }
    }

    static final class Jdk24OrEarlier implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return JavaVersion.current() <= JavaVersion.VERSION_24
        }
    }

    static final class JdkOracle implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            System.getProperty('java.vm.vendor') == 'Oracle Corporation'
        }
    }

    static final class Online implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            try {
                new URL("http://google.com").openConnection().getInputStream().close()
                return true
            } catch (IOException ex) {
                return false
            }
        }
    }

    static final class CanInstallExecutable implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return doSatisfies(FilePermissions) || doSatisfies(Windows)
        }
    }

    static final class SmartTerminalAvailable implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return System.getenv("TERM")?.toUpperCase() != "DUMB"
        }
    }

    static final class HasXCode implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            // Simplistic approach at detecting Xcode by assuming macOS imply Xcode is present
            return doSatisfies(MacOs)
        }
    }

    static final class HasMsBuild implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            // Simplistic approach at detecting MSBuild by assuming Windows imply MSBuild is present
            return doSatisfies(Windows) && "embedded" != System.getProperty("org.gradle.integtest.executer")
        }
    }

    static final class SupportsTargetingJava6 implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(Jdk12OrLater)
        }
    }

    // Currently mac agents are not that strong so we avoid running high-concurrency tests on them
    static final class HighPerformance implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            return notSatisfies(MacOs)
        }
    }

    static final class NotEC2Agent implements TestPrecondition {
        @Override
        boolean isSatisfied() throws UnknownHostException {
            return !InetAddress.getLocalHost().getHostName().startsWith("ip-")
        }
    }

    static final class StableGroovy implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            !GroovySystem.version.endsWith("-SNAPSHOT")
        }
    }

    static final class NotStableGroovy implements TestPrecondition {
        @Override
        boolean isSatisfied() {
            notSatisfies(StableGroovy)
        }
    }

}