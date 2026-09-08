package com.schoolMarket.exception;

public class StockNotEnoughException extends RuntimeException {
    public StockNotEnoughException() { super(); }
    public StockNotEnoughException(String message) { super(message); }
}
