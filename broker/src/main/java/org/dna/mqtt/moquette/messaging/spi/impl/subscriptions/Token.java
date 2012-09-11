package org.dna.mqtt.moquette.messaging.spi.impl.subscriptions;

class Token {

    static final Token EMPTY = new Token("");
    static final Token MULTI = new Token("#");
    static final Token SINGLE = new Token("+");
    String name;

    protected Token(String s) {
        name = s;
    }

    protected String name() {
        return name;
    }

    protected boolean match(Token t) {
        if (t == MULTI || t == SINGLE) {
            return false;
        }

        if (this == MULTI || this == SINGLE) {
            return true;
        }

        return equals(t);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Token other = (Token) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}