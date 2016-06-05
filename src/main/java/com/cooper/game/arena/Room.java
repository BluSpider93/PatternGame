/*
 * Copyright (C) 2016 by Amobee Inc.
 * All Rights Reserved.
 */
package com.cooper.game.arena;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cooper.container.LocalResponse;
import com.cooper.enums.LocalErrorType;

public class Room {

    public static List<Character> OCCUPIABLE_POSITIONS = Arrays.asList('_', 'D', 'O', 'L');

    private final List<List<Character>> map;
    private final String roomName;
    private final Position startingPosition;

    private Map<ActiveCharacter, Position> activeCharacters;

    public Room(String fileDirectory) {

        try {

            this.activeCharacters = new HashMap<>();
            List<String> rows = Files.readAllLines(new File(fileDirectory).toPath());
            String[] header = rows.remove(0).split(",");
            roomName = header[0];
            startingPosition = new Position(new Integer(header[1]), new Integer(header[2]));
            this.map =  getCharactersFromRows(rows);
        } catch (IOException ioEx) {
            throw new RuntimeException("Arena didn't load", ioEx);
        }
    }

    private List<List<Character>> getCharactersFromRows(List<String> rows) {

        return rows.stream().map(row -> {
                    List<Character> charRow = new ArrayList<>();
                    for (char c : row.toCharArray())
                        charRow.add(c);
                    return charRow;
                }).collect(Collectors.toList());
    }

    public String getRoomName() {
        return roomName;
    }

    public LocalResponse addPlayer(ActiveCharacter player) {

        if (!activeCharacters.containsKey(player))
            return new LocalResponse(LocalErrorType.PLAYER_ALREADY_EXISTS);

        activeCharacters.put(player, startingPosition);
        return new LocalResponse();
    }

    public LocalResponse addPlayer(ActiveCharacter player, Position position) {

        if (activeCharacters.containsKey(player))
            return new LocalResponse(LocalErrorType.PLAYER_ALREADY_EXISTS);
        if (isPlayerInSpace(position))
            return new LocalResponse(LocalErrorType.ANOTHER_PLAYER_OCCUPIES_SPACE);
        if (!OCCUPIABLE_POSITIONS.contains(map.get(position.ROW).get(position.COLUMN)))
            return new LocalResponse(LocalErrorType.PLAYER_CANNOT_OCCUPY_SPACE);

        activeCharacters.put(player, position);
        return new LocalResponse();
    }

    private Boolean isPlayerInSpace(Position position) {
        return activeCharacters.values()
                .stream()
                .map(position::equals)
                .reduce(false, (bool1, bool2) -> bool1 || bool2);
    }

    public LocalResponse movePlayer(ActiveCharacter player, Position position) {

        if (!activeCharacters.containsKey(player))
            return new LocalResponse(LocalErrorType.PLAYER_NOT_IN_ROOM);
        if (!OCCUPIABLE_POSITIONS.contains(map.get(position.ROW).get(position.COLUMN)))
            return new LocalResponse(LocalErrorType.PLAYER_CANNOT_OCCUPY_SPACE);

        activeCharacters.replace(player, position);
        return new LocalResponse();
    }

    public LocalResponse removePlayer(ActiveCharacter player) {

        if (activeCharacters.containsKey(player)) {
            activeCharacters.remove(player);
            return new LocalResponse();
        }
        return new LocalResponse(LocalErrorType.PLAYER_NOT_IN_ROOM);
    }

    public LocalResponse getMap() {

        List<List<Character>> overlaidMap = getCopyOfMap();
        insertCharacterTokensIntoMap(overlaidMap);
        return new LocalResponse(convertMapToString(overlaidMap));
    }

    private List<List<Character>> getCopyOfMap() {

        List<List<Character>> mapCopy = new ArrayList<>();
        map.forEach(row -> {
            List<Character> rowCopy = new ArrayList<>();
            row.forEach(rowCopy::add);
            mapCopy.add(rowCopy);
        });
        return mapCopy;
    }

    private void insertCharacterTokensIntoMap(List<List<Character>> overlaidMap) {

        activeCharacters.forEach((c, p) -> overlaidMap.get(p.ROW).set(p.COLUMN, 'U'));
    }

    private String convertMapToString(List<List<Character>> overlayArray) {

        return overlayArray
                .stream()
                .map(String::valueOf)
                .reduce("", (full, row) -> full + row + "\n");
    }
}
