package oshi.util.platform.unix.sysctl;

public interface ISysctl<T, R> {

    R getSysctl(T name, R def);

}
