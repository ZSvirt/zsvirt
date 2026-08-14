package org.zstack.utils.data;

public enum SizeUnit {
    BYTE {
        static final String FULL_NAME = "Byte";
        static final String NAME = "B";

        @Override
        public long toByte(long s) {
            return s;
        }

        @Override
        @Deprecated
        public long toKiloByte(long s) {
            return (s / (k / b));
        }

        @Override
        @Deprecated
        public long toMegaByte(long s) {
            return (s / (m / b));
        }

        @Override
        @Deprecated
        public long toGigaByte(long s) {
            return (s / (g / b));
        }

        @Override
        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / b));
        }

        @Override
        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toByte(s);
        }

        @Override
        public double toByte(double s) {
            return s;
        }

        @Override
        public double toKiloByte(double s) {
            return (s / ((double) k / b));
        }

        @Override
        public double toMegaByte(double s) {
            return (s / ((double) m / b));
        }

        @Override
        public double toGigaByte(double s) {
            return (s / ((double) g / b));
        }

        @Override
        public double toTeraByte(double s) {
            return (s / ((double) t / b));
        }

        @Override
        public double toPetaByte(double s) {
            return (s / ((double) p / b));
        }

        @Override
        public double convert(double s, SizeUnit src) {
            return src.toByte(s);
        }

        @Override
        public long getUnitValue() {
            return b;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getFullName() {
            return FULL_NAME;
        }
    },
    KILOBYTE {
        static final String FULL_NAME = "Kilobyte";
        static final String NAME = "KB";

        @Override
        public long toByte(long s) {
            return (s * (k / b));
        }

        @Override
        @Deprecated
        public long toKiloByte(long s) {
            return s;
        }

        @Override
        @Deprecated
        public long toMegaByte(long s) {
            return (s / (m / k));
        }

        @Override
        @Deprecated
        public long toGigaByte(long s) {
            return (s / (g / k));
        }

        @Override
        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / k));
        }

        @Override
        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toKiloByte(s);
        }

        @Override
        public double toByte(double s) {
            return (s * ((double) k / b));
        }

        @Override
        public double toKiloByte(double s) {
            return s;
        }

        @Override
        public double toMegaByte(double s) {
            return (s / ((double) m / k));
        }

        @Override
        public double toGigaByte(double s) {
            return (s / ((double) g / k));
        }

        @Override
        public double toTeraByte(double s) {
            return (s / ((double) t / k));
        }

        @Override
        public double toPetaByte(double s) {
            return (s / ((double) p / k));
        }

        @Override
        public double convert(double s, SizeUnit src) {
            return src.toKiloByte(s);
        }

        @Override
        public long getUnitValue() {
            return k;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getFullName() {
            return FULL_NAME;
        }
    },
    MEGABYTE {
        static final String FULL_NAME = "Megabyte";
        static final String NAME = "MB";

        @Override
        public long toByte(long s) {
            return (s * (m / b));
        }

        @Override
        @Deprecated
        public long toKiloByte(long s) {
            return (s * (m / k));
        }

        @Override
        @Deprecated
        public long toMegaByte(long s) {
            return s;
        }

        @Override
        @Deprecated
        public long toGigaByte(long s) {
            return (s / (g / m));
        }

        @Override
        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / m));
        }

        @Override
        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toMegaByte(s);
        }

        @Override
        public double toByte(double s) {
            return (s * ((double) m / b));
        }

        @Override
        public double toKiloByte(double s) {
            return (s * ((double) m / k));
        }

        @Override
        public double toMegaByte(double s) {
            return s;
        }

        @Override
        public double toGigaByte(double s) {
            return (s / ((double) g / m));
        }

        @Override
        public double toTeraByte(double s) {
            return (s / ((double) t / m));
        }

        @Override
        public double toPetaByte(double s) {
            return (s / ((double) p / m));
        }

        @Override
        public double convert(double s, SizeUnit src) {
            return src.toMegaByte(s);
        }

        @Override
        public long getUnitValue() {
            return m;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getFullName() {
            return FULL_NAME;
        }
    },
    GIGABYTE {
        static final String FULL_NAME = "Gigabyte";
        static final String NAME = "GB";

        @Override
        public long toByte(long s) {
            return (s * (g / b));
        }

        @Override
        @Deprecated
        public long toKiloByte(long s) {
            return (s * (g / k));
        }

        @Override
        @Deprecated
        public long toMegaByte(long s) {
            return (s * (g / m));
        }


        @Override
        @Deprecated
        public long toGigaByte(long s) {
            return s;
        }

        @Override
        @Deprecated
        public long toTeraByte(long s) {
            return (s / (t / g));
        }

        @Override
        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toGigaByte(s);
        }

        @Override
        public double toByte(double s) {
            return (s * ((double) g / b));
        }

        @Override
        public double toKiloByte(double s) {
            return (s * ((double) g / k));
        }

        @Override
        public double toMegaByte(double s) {
            return (s * ((double) g / m));
        }

        @Override
        public double toGigaByte(double s) {
            return s;
        }

        @Override
        public double toTeraByte(double s) {
            return (s / ((double) t / g));
        }

        @Override
        public double convert(double s, SizeUnit src) {
            return src.toGigaByte(s);
        }

        @Override
        public long getUnitValue() {
            return g;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getFullName() {
            return FULL_NAME;
        }
    },
    TERABYTE {
        static final String FULL_NAME = "Terabyte";
        static final String NAME = "TB";

        @Override
        public long toByte(long s) {
            return (s * (t / b));
        }

        @Override
        @Deprecated
        public long toKiloByte(long s) {
            return (s * (t / k));
        }

        @Override
        @Deprecated
        public long toMegaByte(long s) {
            return (s * (t / m));
        }

        @Override
        @Deprecated
        public long toGigaByte(long s) {
            return (s * (t / g));
        }

        @Override
        @Deprecated
        public long toTeraByte(long s) {
            return s;
        }

        @Override
        @Deprecated
        public long convert(long s, SizeUnit src) {
            return src.toTeraByte(s);
        }

        @Override
        public double toByte(double s) {
            return (s * ((double) t / b));
        }

        @Override
        public double toKiloByte(double s) {
            return (s * ((double) t / k));
        }

        @Override
        public double toMegaByte(double s) {
            return (s * ((double) t / m));
        }

        @Override
        public double toGigaByte(double s) {
            return (s * ((double) t / g));
        }

        @Override
        public double toTeraByte(double s) {
            return s;
        }

        @Override
        public double toPetaByte(double s) {
            return (s / ((double) p / t));
        }

        @Override
        public double convert(double s, SizeUnit src) {
            return src.toTeraByte(s);
        }

        @Override
        public long getUnitValue() {
            return t;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getFullName() {
            return FULL_NAME;
        }
    },
    PETABYTE {
        static final String FULL_NAME = "Petabyte";
        static final String NAME = "PB";

        @Override
        public long toByte(long s) {
            return (s * (p / b));
        }

        @Override
        public double toByte(double s) {
            return (s * ((double) p / b));
        }

        @Override
        public double toKiloByte(double s) {
            return (s * ((double) p / k));
        }

        @Override
        public double toMegaByte(double s) {
            return (s * ((double) p / m));
        }

        @Override
        public double toGigaByte(double s) {
            return (s * ((double) p / g));
        }

        @Override
        public double toTeraByte(double s) {
            return (s * ((double) p / g));
        }

        @Override
        public double toPetaByte(double s) {
            return s;
        }

        @Override
        public double convert(double s, SizeUnit src) {
            return src.toPetaByte(s);
        }

        @Override
        public long getUnitValue() {
            return p;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getFullName() {
            return FULL_NAME;
        }
    };

    public static SizeUnit fromString(String s) {
        if ("b".equalsIgnoreCase(s)) {
            return BYTE;
        } else if ("k".equalsIgnoreCase(s)) {
            return KILOBYTE;
        } else if ("m".equalsIgnoreCase(s)) {
            return MEGABYTE;
        } else if ("g".equalsIgnoreCase(s)) {
            return GIGABYTE;
        } else if ("t".equalsIgnoreCase(s)) {
            return TERABYTE;
        } else if ("p".equalsIgnoreCase(s)) {
            return PETABYTE;
        } else {
            throw new IllegalArgumentException(String.format("unknown size unit[%s]", s));
        }
    }

    private static final long b = 1;
    private static final long k = b * 1024;
    private static final long m = k * 1024;
    private static final long g = m * 1024;
    private static final long t = g * 1024;
    private static final long p = t * 1024;

    public long toByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toKiloByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toMegaByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toGigaByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long toTeraByte(long s) {
        throw new AbstractMethodError();
    }

    @Deprecated
    public long convert(long s, SizeUnit src) {
        // long type will result in wrong number, please use double
        throw new AbstractMethodError();
    }


    public double toByte(double s) {
        throw new AbstractMethodError();
    }
    public double toKiloByte(double s) {
        throw new AbstractMethodError();
    }
    public double toMegaByte(double s) {
        throw new AbstractMethodError();
    }
    public double toGigaByte(double s) {
        throw new AbstractMethodError();
    }
    public double toTeraByte(double s) {
        throw new AbstractMethodError();
    }
    public double toPetaByte(double s) {
        throw new AbstractMethodError();
    }

    public double convert(double s, SizeUnit src) {
        throw new AbstractMethodError();
    }

    public long getUnitValue() {
        throw new AbstractMethodError();
    }

    public String getName() {
        throw new AbstractMethodError();
    }

    public String getFullName() {
        throw new AbstractMethodError();
    }
}
