/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.rest.client.message;

import org.gridgain.grid.portable.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Request for a log file.
 */
public class GridClientLogRequest extends GridClientAbstractMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    public static final int PORTABLE_TYPE_ID = -3;


    /** Task name. */
    private String path;

    /** From line, inclusive, indexing from 0. */
    private int from = -1;

    /** To line, inclusive, indexing from 0, can exceed count of lines in log. */
    private int to = -1;

    /**
     * @return Path to log file.
     */
    public String path() {
        return path;
    }

    /**
     * @param path Path to log file.
     */
    public void path(String path) {
        this.path = path;
    }

    /**
     * @return From line, inclusive, indexing from 0.
     */
    public int from() {
        return from;
    }

    /**
     * @param from From line, inclusive, indexing from 0.
     */
    public void from(int from) {
        this.from = from;
    }

    /**
     * @return To line, inclusive, indexing from 0.
     */
    public int to() {
        return to;
    }

    /**
     * @param to To line, inclusive, indexing from 0.
     */
    public void to(int to) {
        this.to = to;
    }

    /** {@inheritDoc} */
    @Override public int typeId() {
        return PORTABLE_TYPE_ID;
    }

    /** {@inheritDoc} */
    @Override public void writePortable(GridPortableWriter writer) throws IOException {
        super.writePortable(writer);

        writer.writeString("path", path);

        writer.writeInt("from", from);
        writer.writeInt("to", to);
    }

    /** {@inheritDoc} */
    @Override public void readPortable(GridPortableReader reader) throws IOException {
        super.readPortable(reader);

        path = reader.readString("path");

        from = reader.readInt("from");
        to = reader.readInt("to");
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        U.writeString(out, path);

        out.writeInt(from);
        out.writeInt(to);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        path = U.readString(in);

        from = in.readInt();
        to = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        StringBuilder b = new StringBuilder().
            append("GridClientLogRequest [path=").
            append(path);

        if (from != -1)
            b.append(", from=").append(from);

        if (to != -1)
            b.append(", to=").append(to);

        b.append(']');

        return b.toString();
    }
}