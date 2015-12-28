package com.meetme.cameracolors;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by bherbert on 12/28/15.
 */
public class ChunkedMessage {
    List<String> chunks = new ArrayList<>();
    List<Boolean> validated = new ArrayList();

    String lastChunk = null;

    public ChunkedMessage() {

    }

    public void add(String chunk) {
        if (chunk == null) return;

        if (lastChunk != null && chunk.equals(lastChunk) &&
                (chunks.isEmpty() || !chunks.get(chunks.size() - 1).equals(chunk))) {

            chunks.add(chunk);
        } else if (chunks != null && chunks.get(0).equals(chunks)) {

        }

        lastChunk = chunk;
    }
}
