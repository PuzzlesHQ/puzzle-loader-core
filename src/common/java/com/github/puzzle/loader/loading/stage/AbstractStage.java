package com.github.puzzle.loader.loading.stage;

import com.github.puzzle.loader.loading.GameLoader;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractStage implements GameLoader.Stage {

    protected GameLoader loader;
    protected List<Runnable> tasks = new ArrayList<>();
    protected String name;

    public AbstractStage(String stageName) {
        this.name = stageName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setGameLoader(GameLoader loader) {
        this.loader = loader;
    }

    @Override
    public GameLoader getGameLoader() {
        return loader;
    }

    @Override
    public List<Runnable> glTasks() {
        return tasks;
    }
}
