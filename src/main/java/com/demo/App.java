package com.demo;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.GState;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc.SaveMode;
import com.pdftron.pdf.PDFNet;
import io.github.cdimascio.dotenv.Dotenv;

public class App {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        String token = dotenv.get("PDFTRON_KEY");
        PDFNet.initialize(token);

        PDFDoc doc = new PDFDoc();
        Page page = doc.pageCreate();
        doc.pagePushBack(page);

        ElementBuilder builder = new ElementBuilder();
        ElementWriter writer = new ElementWriter();
        writer.begin(page);

        ColorSpace sepCyan = createSpotColorSpace(doc, "Cyan",    new float[]{1, 0, 0, 0});
        ColorSpace sepMagenta = createSpotColorSpace(doc, "Magenta", new float[]{0, 1, 0, 0});
        ColorSpace sepYellow = createSpotColorSpace(doc, "Yellow",  new float[]{0, 0, 1, 0});
        ColorSpace sepBlack = createSpotColorSpace(doc, "Black",   new float[]{0, 0, 0, 1});

        float radius = 60;
        float tint = 1f;

        float centerX = 200;
        float centerY = 250;
        float spacing = 70;

        float cxLeft = centerX - spacing / 2f;
        float cxRight = centerX + spacing / 2f;
        float cyTop = centerY + spacing / 2f;
        float cyBottom = centerY - spacing / 2f;

        drawFilledCircle(writer, builder, sepCyan, tint, cxLeft,  cyTop,    radius);

        drawFilledCircle(writer, builder, sepMagenta, tint, cxRight, cyTop,    radius);

        drawFilledCircle(writer, builder, sepYellow,  tint, cxLeft,  cyBottom, radius);

        drawFilledCircle(writer, builder, sepBlack,   tint, cxRight, cyBottom, radius);

        writer.end();

        String outputPath = System.getProperty("user.dir") + "/src/main/resources/separation_circles.pdf";
        doc.save(outputPath, SaveMode.LINEARIZED, null);
        doc.close();
        PDFNet.terminate();
    }

    private static ColorSpace createSpotColorSpace(PDFDoc doc, String name, float[] cmykTarget) throws Exception {
        if (cmykTarget.length != 4) {
            throw new IllegalArgumentException("cmykTarget must have 4 components");
        }

        Obj sepArray = doc.createIndirectArray();
        sepArray.pushBackName("Separation");
        sepArray.pushBackName(name);
        sepArray.pushBackName("DeviceCMYK");

        String psFunction = String.format(
            "{ dup %f mul exch dup %f mul exch dup %f mul exch %f mul }",
            cmykTarget[0], cmykTarget[1], cmykTarget[2], cmykTarget[3]
        );

        byte[] psBytes = psFunction.getBytes("UTF-8");
        Obj tintFunc = doc.createIndirectStream(psBytes);
        tintFunc.putNumber("FunctionType", 4);

        Obj domain = tintFunc.putArray("Domain");
        domain.pushBackNumber(0);
        domain.pushBackNumber(1);

        Obj range = tintFunc.putArray("Range");
        for (int i = 0; i < 4; ++i) {
            range.pushBackNumber(0);
            range.pushBackNumber(1);
        }

        sepArray.pushBack(tintFunc);

        return new ColorSpace(sepArray);
    }

    private static void drawFilledCircle(ElementWriter writer,
                                         ElementBuilder builder,
                                         ColorSpace sepCS,
                                         float tint,
                                         float cx,
                                         float cy,
                                         float r) throws Exception {

        Element circle = builder.createEllipse(cx, cy, r, r);
        circle.setPathFill(true);
        circle.setPathStroke(false);

        GState gs = circle.getGState();

        gs.setFillColorSpace(sepCS);

        gs.setFillOverprint(true);

        circle.getGState().setFillColor(new ColorPt((double) tint, 0.0, 0.0, 0.0));

        writer.writeElement(circle);
    }
}