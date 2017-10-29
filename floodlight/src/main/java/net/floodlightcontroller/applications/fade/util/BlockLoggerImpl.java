package net.floodlightcontroller.applications.fade.util;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * block logger implementation.
 * We use a prefix (uniqLogPrefix) to identify a type of log messages,
 * and we would automatically assign an unique id (uniqLogId) for this times of log action.
 * We assure that the uniqLogId would never used again if there is so much much logs (long value).
 */
public class BlockLoggerImpl implements BlockLogger {
    private static AtomicLong nextUniqLogId = new AtomicLong(1);
    private static final String LOGGER_START = "Logger started";
    private static final String LOGGER_END = "Logger completed";
    public enum LogMode {
        ERROR, WARN, INFO, DEBUG, TRACE;
    };

    private Logger logger;
    private LogMode mode;
    private long uniqId;
    private String uniqPrefix;
    private String logPrefix;

    public BlockLoggerImpl (String uniqPrefix, LogMode mode, Logger logger){
        this.uniqPrefix = uniqPrefix;
        this.logger = logger;
        this.uniqId = requestUniqLogId(this.uniqPrefix);
        this.logPrefix = this.buildLogPrefix();
        switch (mode){
            case ERROR:
                this.mode = (logger.isErrorEnabled() ? mode : null);
                this.error(LOGGER_START);
                break;
            case WARN:
                this.mode = (logger.isWarnEnabled() ? mode : null);
                this.warn(LOGGER_START);
                break;
            case INFO:
                this.mode = (logger.isInfoEnabled() ? mode : null);
                this.info(LOGGER_START);
                break;
            case DEBUG:
                this.mode = (logger.isDebugEnabled() ? mode : null);
                this.debug(LOGGER_START);
                break;
            case TRACE:
                this.mode = (logger.isTraceEnabled() ? mode : null);
                this.trace(LOGGER_START);
                break;
            default:
                this.mode = null;
                break;
        }
    }

    @Override
    public void close() throws IOException {
        switch (this.mode){
            case ERROR:
                this.error(LOGGER_END);
                break;
            case WARN:
                this.warn(LOGGER_END);
                break;
            case INFO:
                this.info(LOGGER_END);
                break;
            case DEBUG:
                this.debug(LOGGER_END);
                break;
            case TRACE:
                this.trace(LOGGER_END);
                break;
            default:
                break;
        }
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return LogMode.TRACE == this.mode && this.logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if(LogMode.TRACE == this.mode){
            this.logger.trace(this.buildNewMessageOrFormat(msg));
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if(LogMode.TRACE == this.mode){
            this.logger.trace(this.buildNewMessageOrFormat(format), arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if(LogMode.TRACE == this.mode){
            this.logger.trace(this.buildNewMessageOrFormat(format), arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object[] argArray) {
        if(LogMode.TRACE == this.mode){
            this.logger.trace(this.buildNewMessageOrFormat(format), argArray);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if(LogMode.TRACE == this.mode){
            this.logger.trace(this.buildNewMessageOrFormat(msg), t);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDebugEnabled() {
        return (LogMode.TRACE == this.mode || LogMode.DEBUG == this.mode) && this.logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if(this.mode == LogMode.DEBUG || this.mode == LogMode.TRACE) {
            this.logger.debug(this.buildNewMessageOrFormat(msg));
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if(this.mode == LogMode.DEBUG || this.mode == LogMode.TRACE) {
            this.logger.debug(this.buildNewMessageOrFormat(format), arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if(this.mode == LogMode.DEBUG || this.mode == LogMode.TRACE) {
            this.logger.debug(this.buildNewMessageOrFormat(format), arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object[] argArray) {
        if(this.mode == LogMode.DEBUG || this.mode == LogMode.TRACE) {
            this.logger.debug(this.buildNewMessageOrFormat(format), argArray);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if(this.mode == LogMode.DEBUG || this.mode == LogMode.TRACE) {
            this.logger.debug(this.buildNewMessageOrFormat(msg), t);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInfoEnabled() {
        return (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO) && this.logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO){
            this.logger.info(this.buildNewMessageOrFormat(msg));
        }
    }

    @Override
    public void info(String format, Object arg) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO){
            this.logger.info(this.buildNewMessageOrFormat(format), arg);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO){
            this.logger.info(this.buildNewMessageOrFormat(format), arg1, arg2);
        }
    }

    @Override
    public void info(String format, Object[] argArray) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO){
            this.logger.info(this.buildNewMessageOrFormat(format), argArray);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO){
            this.logger.info(this.buildNewMessageOrFormat(msg), t);
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWarnEnabled() {
        return (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN) && this.logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN){
            this.logger.warn(this.buildNewMessageOrFormat(msg));
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN){
            this.logger.warn(this.buildNewMessageOrFormat(format), arg);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN){
            this.logger.warn(this.buildNewMessageOrFormat(format), arg1, arg2);
        }
    }

    @Override
    public void warn(String format, Object[] argArray) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN){
            this.logger.warn(this.buildNewMessageOrFormat(format), argArray);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if(this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN){
            this.logger.warn(this.buildNewMessageOrFormat(msg), t);
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isErrorEnabled() {
        return (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN || this.mode == LogMode.ERROR)
                && this.logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN || this.mode == LogMode.ERROR){
            logger.error(this.buildNewMessageOrFormat(msg));
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN || this.mode == LogMode.ERROR){
            logger.error(this.buildNewMessageOrFormat(format), arg);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN || this.mode == LogMode.ERROR){
            logger.error(this.buildNewMessageOrFormat(format), arg1, arg2);
        }
    }

    @Override
    public void error(String format, Object[] argArray) {
        if (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN || this.mode == LogMode.ERROR){
            logger.error(this.buildNewMessageOrFormat(format), argArray);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (this.mode == LogMode.TRACE || this.mode == LogMode.DEBUG || this.mode == LogMode.INFO || this.mode == LogMode.WARN || this.mode == LogMode.ERROR){
            logger.error(this.buildNewMessageOrFormat(msg), t);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }


    private static long requestUniqLogId(String uniqPrefix){
        return nextUniqLogId.getAndIncrement();
    }

    private String buildLogPrefix() {
        StringBuilder sb = new StringBuilder();
        sb.append("[BlockLogger");
        sb.append(" prefix=").append(this.uniqPrefix);
        sb.append(" id=").append(this.uniqId);
        sb.append("]");
        return sb.toString();
    }

    private String buildNewMessageOrFormat(String msgOrFormat){
        return this.logPrefix + " " + msgOrFormat;
    }
}
