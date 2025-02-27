/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.model;

import android.content.Context;

import androidx.annotation.NonNull;

import com.emanuelef.remote_capture.AppsResolver;
import com.emanuelef.remote_capture.R;

import java.io.Serializable;

/* Equivalent of zdtun_conn_t from zdtun and conn_data_t from vpnproxy.c */
public class ConnectionDescriptor implements Serializable {
    // sync with zdtun_conn_status_t

    public static final int CONN_STATUS_NEW = 0,
        CONN_STATUS_CONNECTING = 1,
        CONN_STATUS_CONNECTED = 2,
        CONN_STATUS_CLOSED = 3,
        CONN_STATUS_ERROR = 4,
        CONN_STATUS_SOCKET_ERROR = 5,
        CONN_STATUS_CLIENT_ERROR = 6,
        CONN_STATUS_RESET = 7,
        CONN_STATUS_UNREACHABLE = 8;

    public enum Status {
        STATUS_INVALID,
        STATUS_OPEN,
        STATUS_CLOSED,
        STATUS_UNREACHABLE,
        STATUS_ERROR,
    }

    /* Metadata */
    public int ipver;
    public int ipproto;
    public String src_ip;
    public String dst_ip;
    public int src_port;
    public int dst_port;

    /* Data */
    public long first_seen;
    public long last_seen;
    public long sent_bytes;
    public long rcvd_bytes;
    public int sent_pkts;
    public int rcvd_pkts;
    public String info;
    public String url;
    public String request_plaintext;
    public String l7proto;
    public int uid;
    public int incr_id;
    public int status;
    private int tcp_flags;
    private boolean blacklisted_ip;
    private boolean blacklisted_host;

    /* Internal */
    public boolean alerted;
    private boolean whitelisted_ip;
    private boolean whitelisted_host;
    private boolean whitelisted_app;

    public ConnectionDescriptor(int _incr_id, int _ipver, int _ipproto, String _src_ip, String _dst_ip,
                                int _src_port, int _dst_port, int _uid, long when) {
        incr_id = _incr_id;
        ipver = _ipver;
        ipproto = _ipproto;
        src_ip = _src_ip;
        dst_ip = _dst_ip;
        src_port = _src_port;
        dst_port = _dst_port;
        uid = _uid;
        first_seen = last_seen = when;
    }

    public void processUpdate(ConnectionUpdate update) {
        // The "update_type" is used to limit the amount of data sent via the JNI
        if((update.update_type & ConnectionUpdate.UPDATE_STATS) != 0) {
            sent_bytes = update.sent_bytes;
            rcvd_bytes = update.rcvd_bytes;
            sent_pkts = update.sent_pkts;
            rcvd_pkts = update.rcvd_pkts;
            status = (update.status & 0x00FF);
            blacklisted_ip = (update.status & 0x0100) != 0;
            blacklisted_host = (update.status & 0x0200) != 0;
            last_seen = update.last_seen;
            tcp_flags = update.tcp_flags;
        }
        if((update.update_type & ConnectionUpdate.UPDATE_INFO) != 0) {
            info = update.info;
            url = update.url;
            request_plaintext = update.request_plaintext;
            l7proto = update.l7proto;
        }
    }

    public Status getStatus() {
        if(status >= CONN_STATUS_CLOSED) {
            switch(status) {
                case CONN_STATUS_CLOSED:
                case CONN_STATUS_RESET:
                    return Status.STATUS_CLOSED;
                case CONN_STATUS_UNREACHABLE:
                    return Status.STATUS_UNREACHABLE;
                default:
                    return Status.STATUS_ERROR;
            }
        }
        return Status.STATUS_OPEN;
    }

    public static String getStatusLabel(Status status, Context ctx) {
        int resid;

        switch(status) {
            case STATUS_OPEN: resid = R.string.conn_status_open; break;
            case STATUS_CLOSED: resid = R.string.conn_status_closed; break;
            case STATUS_UNREACHABLE: resid = R.string.conn_status_unreachable; break;
            default: resid = R.string.error;
        }

        return(ctx.getString(resid));
    }

    public String getStatusLabel(Context ctx) {
        return getStatusLabel(getStatus(), ctx);
    }

    public boolean matches(AppsResolver res, String filter) {
        filter = filter.toLowerCase();
        AppDescriptor app = res.get(uid, 0);

        return(((info != null) && (info.contains(filter))) ||
                dst_ip.contains(filter) ||
                l7proto.toLowerCase().contains(filter) ||
                Integer.toString(uid).equals(filter) ||
                Integer.toString(dst_port).contains(filter) ||
                Integer.toString(src_port).equals(filter) ||
                ((app != null) && (app.getName().toLowerCase().contains(filter) ||
                        app.getPackageName().equals(filter)))
        );
    }

    public int getSentTcpFlags() {
        return (tcp_flags >> 8);
    }

    public int getRcvdTcpFlags() {
        return (tcp_flags & 0xFF);
    }

    public boolean isBlacklistedIp() { return !whitelisted_app && !whitelisted_ip && blacklisted_ip; }
    public boolean isBlacklistedHost() { return !whitelisted_app && !whitelisted_host && blacklisted_host; }
    public boolean isBlacklisted() {
        return isBlacklistedIp() || isBlacklistedHost();
    }

    public void updateWhitelist(MatchList whitelist) {
        whitelisted_app = whitelist.matchesApp(uid);
        whitelisted_ip = whitelist.matchesIP(dst_ip);
        whitelisted_host = whitelist.matchesHost(info);
    }

    @Override
    public @NonNull String toString() {
        return "[proto=" + ipproto + "/" + l7proto + "]: " + src_ip + ":" + src_port + " -> " +
                dst_ip + ":" + dst_port + " [" + uid + "] " + info;
    }
}
