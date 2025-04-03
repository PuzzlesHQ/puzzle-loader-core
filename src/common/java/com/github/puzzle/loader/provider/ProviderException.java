package com.github.puzzle.loader.provider;

public class ProviderException extends Exception {

    public ProviderException(String s) {
        super(s);
    }

    public ProviderException(Throwable t) {
        super(t);
    }

    public ProviderException(String s, Throwable t) {
        super(s, t);
    }
}
