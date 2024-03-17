package com.codecritical.lib.model;


/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import eu.printingin3d.javascad.coords.Abstract3d;
import eu.printingin3d.javascad.coords.Coords3d;

import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;

public class MapPoint3D {

    public final double i, j, k;
    public final int iLength;

    public MapPoint3D(double i, double j, double k) {
        this.i = i;
        this.j = j;
        this.k = k;
        iLength = 1;
    }

    public MapPoint3D(double i, double j, double k, int iLength) {
        this.i = i;
        this.j = j;
        this.k = k;
        this.iLength = iLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapPoint3D mapPoint = (MapPoint3D) o;
        return i == mapPoint.i && j == mapPoint.j && k == mapPoint.k;
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j, k);
    }

    public Abstract3d getCoors3d() {
        return new Coords3d(i, j, k);
    }

    public Builder mutate() {
        return new Builder(this);
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("i", i)
                .add("j", j)
                .add("k", k)
                .add("iLength", iLength)
                .toString();
    }

    public static class Builder {
        private double i;
        private double j;
        private double k;
        private int iLength;

        private Builder(MapPoint3D p) {
            this.i = p.i;
            this.j = p.j;
            this.k = p.k;
            this.iLength = p.iLength;
        }

        public Builder setI(double i) {
            this.i = i;
            return this;
        }

        public Builder setJ(double j) {
            this.j = j;
            return this;
        }

        public Builder setK(double k) {
            this.k = k;
            return this;
        }

        public Builder setI(Function<Double, Double> f) {
            this.i = f.apply(i);
            return this;
        }

        public Builder setJ(Function<Double, Double> f) {
            this.j = f.apply(j);
            return this;
        }

        public Builder setK(Function<Double, Double> f) {
            this.k = f.apply(k);
            return this;
        }

        public Builder decI() {
            this.i -= 1.0;
            return this;
        }

        public Builder incI() {
            this.i += 1.0;
            return this;
        }

        public Builder setILength(int iLength) {
            this.iLength = iLength;
            return this;
        }
        public MapPoint3D build() {
            return new MapPoint3D(i, j, k, iLength);
        }
    }

}
