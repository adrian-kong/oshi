package oshi.util.platform.unix.sysctl;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.mac.SystemB;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@ThreadSafe
public class SysctlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SysctlUtil.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private static final Map<Class<?>, ISysctl> methodMap = new HashMap<Class<?>, ISysctl>() {{
        put(Integer.class, (name, def) -> {
            LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(com.sun.jna.platform.mac.SystemB.INT_SIZE);
            Pointer p = new Memory(size.longValue());
            if (0 != SystemB.INSTANCE.sysctlbyname((String) name, p, size, null, LibCAPI.size_t.ZERO)) {
                LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            return p.getInt(0);
        });

        put(Long.class, (name, def) -> {
            LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(com.sun.jna.platform.mac.SystemB.UINT64_SIZE);
            Pointer p = new Memory(size.longValue());
            if (0 != SystemB.INSTANCE.sysctlbyname((String) name, p, size, null, LibCAPI.size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            return p.getLong(0);
        });

        put(String.class, (name, def) -> {
            // Call first time with null pointer to get value of size
            LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference();
            if (0 != SystemB.INSTANCE.sysctlbyname((String) name, null, size, null, LibCAPI.size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            // Add 1 to size for null terminated string
            Pointer p = new Memory(size.longValue() + 1L);
            if (0 != SystemB.INSTANCE.sysctlbyname((String) name, p, size, null, LibCAPI.size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            return p.getString(0);
        });

    }};

    public static <T> T sysctl(String name, T def) {
        return (T) methodMap.get(def.getClass()).getSysctl(name, def);
    }
}
