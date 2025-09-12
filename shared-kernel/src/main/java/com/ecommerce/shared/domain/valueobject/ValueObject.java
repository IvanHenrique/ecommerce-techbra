package com.ecommerce.shared.domain.valueobject;

public abstract class ValueObject {
    
    @Override
    public abstract boolean equals(Object obj);
    
    @Override
    public abstract int hashCode();
    
    protected void validate() {
        // Override in subclasses for validation logic
    }
}