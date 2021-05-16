//
//  ConsoleLogEntryDispatcher.java
//
//  Lunar Unity Mobile Console
//  https://github.com/SpaceMadness/lunar-unity-console
//
//  Copyright 2015-2021 Alex Lementuev, SpaceMadness.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//


package spacemadness.com.lunarconsole.console;

import java.util.ArrayList;
import java.util.List;

import spacemadness.com.lunarconsole.concurrent.DispatchQueue;
import spacemadness.com.lunarconsole.concurrent.DispatchTask;
import spacemadness.com.lunarconsole.debug.Log;

/**
 * Class for handling batches of console entries on UI-thread
 */
class ConsoleLogEntryDispatcher {
    private final OnDispatchListener listener;
    private final DispatchQueue dispatchQueue;
    private final DispatchTask dispatchTask;

    private final List<Entry> entries;
    private final List<Entry> freeList;

    public ConsoleLogEntryDispatcher(DispatchQueue dispatchQueue, OnDispatchListener listener) {
        if (dispatchQueue == null) {
            throw new NullPointerException("Dispatch queue is null");
        }
        if (listener == null) {
            throw new NullPointerException("Listener is null");
        }

        this.dispatchQueue = dispatchQueue;
        this.listener = listener;
        this.entries = new ArrayList<>();
        this.freeList = new ArrayList<>();
        this.dispatchTask = createDispatchTask();
    }

    public void add(byte type, String message, String stackTrace) {
        synchronized (entries) {
            Entry data = createEntryData(type, message, stackTrace);
            entries.add(data);

            if (entries.size() == 1) {
                postEntriesDispatch();
            }
        }
    }

    protected void postEntriesDispatch() {
        dispatchQueue.dispatchOnce(dispatchTask);
    }

    protected void cancelEntriesDispatch() {
        dispatchTask.cancel();
    }

    protected void dispatchEntries() {
        synchronized (entries) {
            try {
                listener.onDispatchEntries(entries);
            } catch (Exception e) {
                Log.e(e, "Can't dispatch entries");
            }
            freeList.addAll(entries);
            entries.clear();
        }
    }

    private DispatchTask createDispatchTask() {
        return new DispatchTask() {
            @Override
            protected void execute() {
                dispatchEntries();
            }
        };
    }

    public void cancelAll() {
        cancelEntriesDispatch();

        synchronized (entries) {
            entries.clear();
        }
    }

    private Entry createEntryData(byte type, String message, String stackTrack) {
        Entry data;
        if (freeList.size() > 0) {
            data = freeList.remove(freeList.size() - 1);
        } else {
            data = new Entry();
        }
        data.type = type;
        data.message = message;
        data.stackTrace = stackTrack;
        return data;
    }

    public interface OnDispatchListener {
        void onDispatchEntries(List<Entry> entries);
    }

    public static class Entry {
        public byte type;
        public String message;
        public String stackTrace;
    }
}
