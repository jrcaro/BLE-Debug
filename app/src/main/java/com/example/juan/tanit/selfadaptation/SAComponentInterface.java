package com.example.juan.tanit.selfadaptation;


// This interface must be implemented by those components of our system that can be reconfigured.
public interface SAComponentInterface {
    public Boolean putInSafeState();
    public Boolean disable();
    public Boolean enable();
    public Boolean tuneParameter(String key,Object param);
}
