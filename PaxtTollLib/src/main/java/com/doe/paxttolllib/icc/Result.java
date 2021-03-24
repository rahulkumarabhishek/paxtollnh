package com.doe.paxttolllib.icc;

public abstract class Result<T> {

    protected T data;

    private Result() {
    }

    public static final class Success<T> extends Result<T> {
        public T data;

        public Success(T data) {
            super.data=data;
            this.data = data;
        }
    }

    public static final class Error<T> extends Result<T> {
        public Exception exception;
        public Error(Exception exception) {
            this.exception = exception;
        }
    }

    public T getData(){
        return data;
    }
}
