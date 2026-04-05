package com.oddlabs.matchserver.models;

import java.sql.Timestamp;
import java.util.ArrayList;

public final class GameDataModel {

    // Basic game information
    private Timestamp timeCreate;
    private String name;
    private String rated;
    private Integer speed;
    private Integer size;
    private Integer hills;
    private Integer trees;
    private Integer resources;
    private String mapcode;
    private String status;
    private Integer id;
    private Integer winner;
    private Timestamp timeStop;
    private Timestamp timeStart;

    ArrayList<GamePlayerModel> players = new ArrayList<>();

    // Constructor
    public GameDataModel() {}

    // Getters and Setters for basic game information
    public Timestamp getTimeCreate() {
        return timeCreate;
    }

    public void setTimeCreate(Timestamp timeCreate) {
        this.timeCreate = timeCreate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRated() {
        return rated;
    }

    public void setRated(String rated) {
        this.rated = rated;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getHills() {
        return hills;
    }

    public void setHills(Integer hills) {
        this.hills = hills;
    }

    public Integer getTrees() {
        return trees;
    }

    public void setTrees(Integer trees) {
        this.trees = trees;
    }

    public Integer getResources() {
        return resources;
    }

    public void setResources(Integer resources) {
        this.resources = resources;
    }

    public String getMapcode() {
        return mapcode;
    }

    public void setMapcode(String mapcode) {
        this.mapcode = mapcode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWinner() {
        return winner;
    }

    public void setWinner(Integer winner) {
        this.winner = winner;
    }

    public Timestamp getTimeStop() {
        return timeStop;
    }

    public void setTimeStop(Timestamp timeStop) {
        this.timeStop = timeStop;
    }

    public Timestamp getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Timestamp timeStart) {
        this.timeStart = timeStart;
    }

    public ArrayList<GamePlayerModel> getPlayers() {
        return players;
    }

    public void setPlayers(ArrayList<GamePlayerModel> players) {
        this.players = players;
    }
}
