package com.chocolatey.pmsencoder

import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.Player
import net.pms.io.OutputParams
import org.apache.log4j.Level

/*
 * this object encapsulates the per-request state passed from/to the PMS transcode launcher (PMSEncoder.java).
 */
@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
public class Command {
    private static final Level DEFAULT_STASH_ASSIGNMENT_LOG_LEVEL = Level.DEBUG
    private Level stashAssignmentLogLevel = Level.DEBUG
    private Stash oldStash

    DLNAMediaInfo media
    DLNAResource dlna
    Event event = Event.TRANSCODE
    List<String> downloader = []
    List<String> hook = []
    List<String> matches = []
    List<String> transcoder = []
    OutputParams params
    Player player
    Stash stash

    public Command() {
        this(new Stash())
    }

    public Command(Stash stash) {
        this.stash = stash
    }

    public Command(List<String> transcoder) {
        this(new Stash(), transcoder)
    }

    // convenience constructor: allows the stash to be supplied as a Map
    // e.g. new Command([ uri: uri ])
    public Command(Map<Object, Object> map) {
        this.stash = new Stash(map)
    }

    // this is needed to appease CompileStatic
    public Command(Stash stash, List<String> transcoder) {
        this.stash = stash
        this.transcoder = transcoder
    }

    public Command(Map<Object, Object> map, List<String> transcoder) {
        this.stash = new Stash(map)
        this.transcoder = transcoder
    }

    public java.lang.String toString() {
        def repr = """
        {
            event:                 $event
            matches:               ${matches.inspect()}
            hook:                  ${hook.inspect()}
            downloader:            ${downloader.inspect()}
            transcoder:            ${transcoder.inspect()}
            stash:                 ${stash.inspect()}
        }""".substring(1).stripIndent(8)
    }

    public void deferStashChanges() {
        assert oldStash == null
        oldStash = stash
        stash = new Stash(stash)
        // avoid clogging up the logfile with pattern-block stash assignments. If the pattern doesn't match,
        // the assignments are irrelevant; and if it does match, the assignments are logged later
        // (when the pattern's temporary stash is merged into the command stash). Rather than suppressing these
        // assignments completely, log them at the lowest (TRACE) level
        stashAssignmentLogLevel = Level.TRACE
    }

    public void discardStashChanges() {
        assert oldStash != null
        stash = oldStash
        oldStash = null
        stashAssignmentLogLevel = DEFAULT_STASH_ASSIGNMENT_LOG_LEVEL
    }

    public void commitStashChanges() {
        assert oldStash != null
        def newStash = stash
        stash = oldStash
        oldStash = null
        // merge (with full logging)
        stashAssignmentLogLevel = DEFAULT_STASH_ASSIGNMENT_LOG_LEVEL
        newStash.each { name, value -> setVar(name, value) }
    }

    protected boolean hasVar(Object name) {
        stash.containsKey(name)
    }

    protected Object getVar(Object name) {
        stash.get(name)
    }

    protected String getVarAsString(Object name) {
        def value = stash.get(name)
        return value?.toString()
    }

    // setter implementation with logged stash assignments
    protected Object setVar(Object name, Object value) {
        if (stash.get(name) != value) {
            if (stashAssignmentLogLevel != null) {
                logger.log(stashAssignmentLogLevel, "setting $name to ${value.inspect()}")
            }

            stash.put(name, value)
        }

        return value // for chaining: foo = bar = baz i.e. foo = (bar = baz)
    }
}
