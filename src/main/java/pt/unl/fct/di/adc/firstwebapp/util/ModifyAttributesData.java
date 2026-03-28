package pt.unl.fct.di.adc.firstwebapp.util;

public class ModifyAttributesData {
    public Input input;
    public AuthToken token;

    public ModifyAttributesData() {}

    public static class Input {
        public String username;
        public Attributes attributes;

        public Input() {}

        public static class Attributes {
            public String phone;
            public String address;

            public Attributes() {}
        }
    }

    public boolean validate() {
        return input != null && nonEmptyOrBlankField(input.username) && (nonEmptyOrBlankField(input.attributes.phone)
                || nonEmptyOrBlankField(input.attributes.address));
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }
}
