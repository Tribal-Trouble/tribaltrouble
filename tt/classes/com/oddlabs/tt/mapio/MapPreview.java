package com.oddlabs.tt.mapio;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.InflaterInputStream;

/**
 * Generates a small cached thumbnail for map files, overlaying basic resource markers.
 */
public final class MapPreview {
    private MapPreview() {}

    // Format constants (duplicated from MapIO; keep private/local)
    private static final int MAGIC_TTMP = 0x54544D50; // 'TTMP'
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final int TAG_META = 0x4D455441; // META
    private static final int TAG_HM3Z = 0x484D335A; // HM3Z
    private static final int TAG_HM2Q = 0x484D3251; // HM2Q
    private static final int TAG_ROCK = 0x524F434B; // ROCK
    private static final int TAG_IRON = 0x49524F4E; // IRON
    private static final int TAG_PLTS = 0x504C5453; // PLTS
    private static final int TAG_TREE = 0x54524545; // TREE
    private static final int TAG_RK2  = 0x524B3221; // RK2!
    private static final int TAG_IR2  = 0x49523221; // IR2!
    private static final int TAG_PLT2 = 0x504C5432; // PLT2
    private static final int TAG_TR2  = 0x54523221; // TR2!

    private static final int DEFAULT_PREVIEW_DIM = 256;

    // Minimal GL constants (avoid pulling full GL dependency chain here)
    private static final int GL_RGBA = 0x1908;
    private static final int GL_LINEAR = 0x2601;
    private static final int GL_CLAMP = 0x2900;

    private static final class CacheEntry { final long lm; final Texture tex; CacheEntry(long lm, Texture t){this.lm=lm;this.tex=t;} }
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static Texture BLANK;

    private static final class Point {
        final int gx, gy;          // grid cell (may be -1 for plant absolute)
        final float wx, wy;        // world coordinates (may be NaN if not stored)
        final byte ox, oy;         // packed offsets (for v2 packed sections)
        final boolean hasOffset;
        Point(int gx,int gy,float wx,float wy){ this.gx=gx; this.gy=gy; this.wx=wx; this.wy=wy; this.ox=0; this.oy=0; this.hasOffset=false; }
        Point(int gx,int gy,byte ox,byte oy){ this.gx=gx; this.gy=gy; this.wx=Float.NaN; this.wy=Float.NaN; this.ox=ox; this.oy=oy; this.hasOffset=true; }
    }

    private static final class HeightData {
        float[][] heights; float seaLevel; int metersPerWorld; int gridSize;
        final List<Point> rocks = new ArrayList<>();
        final List<Point> iron  = new ArrayList<>();
        final List<Point> plants= new ArrayList<>();
        final List<Point> trees = new ArrayList<>();
    }

    // -------- Public API ---------

    public static Texture getBlankTexture() {
        if (BLANK != null) return BLANK;
        GLIntImage img = new GLIntImage(4,4,GL_RGBA);
        for (int y=0;y<4;y++) for (int x=0;x<4;x++) img.putPixel(x,y, ((x^y)&1)==0?0x303030FF:0x404040FF);
        GLImage[] mip = img.buildMipMaps();
        GLImage.updateMipMapsArea(mip, 10000, 1f, 0,0,4,4,false);
        BLANK = new Texture(mip, GL_RGBA, GL_LINEAR, GL_LINEAR, GL_CLAMP, GL_CLAMP);
        return BLANK;
    }

    public static Texture getPreviewTexture(File mapFile) {
        if (mapFile == null || !mapFile.exists()) return getBlankTexture();
        String key = mapFile.getAbsolutePath(); long lm = mapFile.lastModified();
        CacheEntry ce = CACHE.get(key); if (ce != null && ce.lm == lm) return ce.tex;
        try {
            HeightData hd = readHeightData(mapFile);
            if (hd == null || hd.heights == null) return getBlankTexture();
            Texture t = buildTexture(hd, DEFAULT_PREVIEW_DIM);
            CACHE.put(key, new CacheEntry(lm, t));
            return t;
        } catch (Throwable ignored) { return getBlankTexture(); }
    }

    // -------- File Parsing (minimal) ---------

    private static int readVarInt(DataInputStream in) throws IOException {
        int shift=0, result=0; while (true) { int b=in.readUnsignedByte(); result |= (b & 0x7F) << shift; if ((b & 0x80)==0) break; shift+=7; if (shift>28) throw new IOException("VarInt too long"); } return result;
    }
    private static float dequantizeCellOffset(byte b){ return b/255f; }

    private static HeightData readHeightData(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            if (in.readInt() != MAGIC_TTMP) return null;
            int ver = in.readInt(); if (ver != VERSION_1 && ver != VERSION_2) return null;
            HeightData hd = new HeightData();
            while (true) {
                int tag,len; try { tag=in.readInt(); len=in.readInt(); } catch (EOFException eof) { break; }
                if (len < 0) break;
                byte[] section = new byte[len]; in.readFully(section);
                switch (tag) {
                    case TAG_META: {
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(section));
                        hd.metersPerWorld = s.readInt();
                        s.readInt(); // terrain type unused here
                        hd.seaLevel = s.readFloat();
                        hd.gridSize = s.readInt();
                        break; }
                    case TAG_HM2Q: {
                        DataInputStream s = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(section)));
                        int n = s.readInt(); float min = s.readFloat(); float max = s.readFloat();
                        float[][] h = new float[n][n]; float scale = (max-min)/65535f;
                        for (int y=0;y<n;y++) for (int x=0;x<n;x++) h[y][x] = min + s.readUnsignedShort()*scale;
                        hd.heights = h; break; }
                    case TAG_HM3Z: {
                        DataInputStream s = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(section)));
                        int n = s.readInt(); float[][] h = new float[n][n];
                        for (int y=0;y<n;y++) for (int x=0;x<n;x++) h[y][x] = s.readFloat();
                        hd.heights = h; break; }
                    case TAG_ROCK: case TAG_IRON: {
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(section)); int count = s.readInt();
                        int avail = s.available(); boolean hasExtras = avail >= count * 16; // heuristic
                        for (int i=0;i<count;i++) {
                            int gx = s.readInt(); int gy = s.readInt(); float wx=Float.NaN, wy=Float.NaN;
                            if (hasExtras) { wx=s.readFloat(); wy=s.readFloat(); s.readFloat(); s.readInt(); }
                            (tag==TAG_ROCK?hd.rocks:hd.iron).add(new Point(gx,gy,wx,wy));
                        } break; }
                    case TAG_PLTS: {
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(section)); int count = s.readInt();
                        int avail = s.available(); boolean hasDir = avail >= count * 16; // approx
                        for (int i=0;i<count;i++) { s.readInt(); float wx=s.readFloat(); float wy=s.readFloat(); if (hasDir){ s.readFloat(); s.readFloat(); } hd.plants.add(new Point(-1,-1,wx,wy)); }
                        break; }
                    case TAG_TREE: {
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(section)); int count = s.readInt();
                        for (int i=0;i<count;i++) { s.readInt(); int gx=s.readInt(); int gy=s.readInt(); float wx=s.readFloat(); float wy=s.readFloat(); hd.trees.add(new Point(gx,gy,wx,wy)); }
                        break; }
                    case TAG_RK2: case TAG_IR2: {
                        DataInputStream s = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(section))); int count = readVarInt(s);
                        for (int i=0;i<count;i++) { int gx=readVarInt(s); int gy=readVarInt(s); byte ox=s.readByte(); byte oy=s.readByte(); s.readUnsignedShort(); readVarInt(s); (tag==TAG_RK2?hd.rocks:hd.iron).add(new Point(gx,gy,ox,oy)); }
                        break; }
                    case TAG_PLT2: {
                        DataInputStream s = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(section))); int count=readVarInt(s); int maskBytes=(count+7)>>>3; byte[] mask=new byte[maskBytes]; s.readFully(mask);
                        int mpw = hd.metersPerWorld>0?hd.metersPerWorld:1;
                        for (int i=0;i<count;i++) { readVarInt(s); int qx=s.readUnsignedShort(); int qy=s.readUnsignedShort(); if(((mask[i>>>3]>>(i&7))&1)!=0) s.readUnsignedShort(); float wx=(qx/65535f)*mpw; float wy=(qy/65535f)*mpw; hd.plants.add(new Point(-1,-1,wx,wy)); }
                        break; }
                    case TAG_TR2: {
                        DataInputStream s = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(section))); int count=readVarInt(s);
                        for (int i=0;i<count;i++) { readVarInt(s); int gx=readVarInt(s); int gy=readVarInt(s); byte ox=s.readByte(); byte oy=s.readByte(); hd.trees.add(new Point(gx,gy,ox,oy)); }
                        break; }
                    default: // ignore
                }
            }
            return hd;
        }
    }

    // -------- Texture Build ---------

    private static Texture buildTexture(HeightData hd, int dim) {
        float[][] h = hd.heights; int n = h.length; if (n == 0) return getBlankTexture();
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int y=0;y<n;y++) for (int x=0;x<n;x++) { float v=h[y][x]; if(v<min)min=v; if(v>max)max=v; }
        float sea = hd.seaLevel;
        GLIntImage img = new GLIntImage(dim,dim,GL_RGBA);
        // Precompute light
        float lx=0.6f, ly=0.4f, lz=0.7f; float llen=(float)Math.sqrt(lx*lx+ly*ly+lz*lz); lx/=llen; ly/=llen; lz/=llen;
        for (int py=0; py<dim; py++) {
            float fy = (py/(float)(dim-1))*(n-1); int y0=(int)fy; int y1=Math.min(n-1,y0+1); float ty=fy-y0;
            for (int px=0; px<dim; px++) {
                float fx = (px/(float)(dim-1))*(n-1); int x0=(int)fx; int x1=Math.min(n-1,x0+1); float tx=fx-x0;
                // bilinear sample
                float h00=h[y0][x0]; float h10=h[y0][x1]; float h01=h[y1][x0]; float h11=h[y1][x1];
                float vx0=h00 + (h10-h00)*tx; float vx1=h01 + (h11-h01)*tx; float v = vx0 + (vx1-vx0)*ty;
                // gradient (forward differences)
                float gxH = h[y0][x1]-h[y0][x0]; float gyH = h[y1][x0]-h[y0][x0];
                float nx = -gxH; float ny=-gyH; float nz=1f; float nlen=(float)Math.sqrt(nx*nx+ny*ny+nz*nz); nx/=nlen; ny/=nlen; nz/=nlen;
                float diffuse = nx*lx + ny*ly + nz*lz; if (diffuse<0) diffuse=0; diffuse = 0.4f + 0.6f*diffuse;
                int r,g,b;
                if (v < sea) { // water
                    float depth = (sea - v) / (max - min + 1e-6f); if (depth>1f) depth=1f;
                    r = (int)(25 + 5*diffuse - 15*depth);
                    g = (int)(70 + 10*diffuse - 35*depth);
                    b = (int)(120 + 20*diffuse - 60*depth);
                } else {
                    float rel = (v - sea)/(max - sea + 1e-6f); if (rel<0) rel=0; if (rel>1) rel=1;
                    if (rel < 0.15f) { // beach
                        float t=rel/0.15f; r=(int)(210 + (180-210)*t); g=(int)(200 + (170-200)*t); b=(int)(160 + (130-160)*t);
                    } else if (rel < 0.65f) { // grass
                        float t=(rel-0.15f)/0.5f; r=(int)(90 + (120-90)*t); g=(int)(130 + (160-130)*t); b=(int)(60 + (90-60)*t);
                    } else { // rock/snow
                        float t=(rel-0.65f)/0.35f; if (t>1) t=1; r=(int)(120 + (230-120)*t); g=r; b=r;
                    }
                    r = clamp((int)(r*diffuse)); g=clamp((int)(g*diffuse)); b=clamp((int)(b*diffuse));
                }
                int pixel = (r<<24)|(g<<16)|(b<<8)|0xFF; // RGBA packed (legacy ordering assumption)
                img.putPixel(px,py,pixel);
            }
        }
        overlayResources(img, hd, dim, n);
        GLImage[] mip = img.buildMipMaps();
        GLImage.updateMipMapsArea(mip, 10000, 1f, 0,0,dim,dim,false);
        return new Texture(mip, GL_RGBA, GL_LINEAR, GL_LINEAR, GL_CLAMP, GL_CLAMP);
    }

    // -------- Resource overlay ---------

    private static void overlayResources(GLIntImage img, HeightData hd, int dim, int n) {
        if (hd == null) return;
        int mpw = hd.metersPerWorld>0?hd.metersPerWorld:hd.gridSize; if (mpw<=0) mpw = n;
        float cell = mpw / (float)Math.max(1, n-1);
        float invM = 1f / (mpw + 1e-6f);
        draw(img, hd.rocks, dim, cell, invM, 0xC8C8C8FF);
        draw(img, hd.iron,  dim, cell, invM, 0xD07020FF);
        draw(img, hd.plants,dim, cell, invM, 0x30C030FF);
        draw(img, hd.trees, dim, cell, invM, 0x0F5A0FFF);
    }

    private static void draw(GLIntImage img, List<Point> pts, int dim, float cell, float invM, int color) {
        if (pts.isEmpty()) return;
        int max = dim - 1;
        for (Point p : pts) {
            float x = p.wx, y = p.wy;
            if (Float.isNaN(x) || Float.isNaN(y)) {
                if (p.hasOffset) { x = p.gx * cell + dequantizeCellOffset(p.ox); y = p.gy * cell + dequantizeCellOffset(p.oy); }
                else if (p.gx >= 0) { x = p.gx * cell; y = p.gy * cell; }
                else continue; // skip
            }
            int px = (int)(x * invM * max + 0.5f); int py = (int)(y * invM * max + 0.5f);
            plotDot(img, px, py, color);
        }
    }

    private static void plotDot(GLIntImage img, int cx, int cy, int color) {
        int w = img.getWidth(), h = img.getHeight();
        for (int dy=-1; dy<=1; dy++) for (int dx=-1; dx<=1; dx++) { int x=cx+dx, y=cy+dy; if(x>=0&&y>=0&&x<w&&y<h) img.putPixel(x,y,color); }
    }

    private static int clamp(int v){ return v<0?0:(Math.min(v,255)); }
}
