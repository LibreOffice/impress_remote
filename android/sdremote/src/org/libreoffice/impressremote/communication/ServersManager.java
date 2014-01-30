package org.libreoffice.impressremote.communication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;

import org.libreoffice.impressremote.Preferences;

public class ServersManager {
    private final Context mContext;

    private final ServersFinder mBluetoothServersFinder;
    private final ServersFinder mTcpServersFinder;

    private final Set<Server> mBlacklistedServers;

    public ServersManager(Context aContext) {
        mContext = aContext;

        mBluetoothServersFinder = new BluetoothServersFinder(mContext);
        mTcpServersFinder = new TcpServersFinder(mContext);

        mBlacklistedServers = new HashSet<Server>();
    }

    public void startServersSearch() {
        mBluetoothServersFinder.startSearch();
        mTcpServersFinder.startSearch();
    }

    public void stopServersSearch() {
        mBluetoothServersFinder.stopSearch();
        mTcpServersFinder.stopSearch();
    }

    public List<Server> getServers() {
        List<Server> aServers = new ArrayList<Server>();

        aServers.addAll(mBluetoothServersFinder.getServers());
        aServers.addAll(mTcpServersFinder.getServers());
        aServers.addAll(getManualAddedTcpServers());

        return filterBlacklistedServers(aServers);
    }

    private List<Server> getManualAddedTcpServers() {
        Map<String, ?> aServersEntries = Preferences
            .getAll(mContext, Preferences.Locations.STORED_SERVERS);

        return buildTcpServers(aServersEntries);
    }

    private List<Server> buildTcpServers(Map<String, ?> aServersEntries) {
        List<Server> aServers = new ArrayList<Server>();

        for (String aServerAddress : aServersEntries.keySet()) {
            String aServerName = (String) aServersEntries.get(aServerAddress);

            aServers.add(Server.newTcpInstance(aServerAddress, aServerName));
        }

        return aServers;
    }

    private List<Server> filterBlacklistedServers(List<Server> aServers) {
        List<Server> aFilteredServers = new ArrayList<Server>();

        for (Server aServer : aServers) {
            if (mBlacklistedServers.contains(aServer)) {
                continue;
            }

            aFilteredServers.add(aServer);
        }

        return aFilteredServers;
    }

    public void addTcpServer(String aAddress, String aName) {
        Preferences.set(mContext, Preferences.Locations.STORED_SERVERS,
            aAddress, aName);
    }

    public void removeServer(Server aServer) {
        if (getManualAddedTcpServers().contains(aServer)) {
            removeManualAddedServer(aServer);

            return;
        }

        blacklistServer(aServer);
    }

    private void removeManualAddedServer(Server aServer) {
        Preferences.remove(mContext, Preferences.Locations.STORED_SERVERS,
            aServer.getAddress());
    }

    private void blacklistServer(Server aServer) {
        mBlacklistedServers.add(aServer);
    }
}
