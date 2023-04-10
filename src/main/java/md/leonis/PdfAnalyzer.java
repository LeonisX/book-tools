package md.leonis;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class PdfAnalyzer {

    private static final String EXE_PATH = "C:\\Users\\user\\Downloads\\commands\\";
    private static final Path PDF_PATH = Paths.get("C:\\Users\\user\\Downloads\\");
    private static final String IM_PATH = "C:\\Program Files\\ImageMagick-7.1.1-Q16-HDRI\\magick.exe";
    private static final String PDFBOX = "pdfbox-app-3.0.0-alpha3.jar";

    public static void main(String[] args) throws IOException, InterruptedException {
        for (File pdfName : Objects.requireNonNull(PDF_PATH.toFile().listFiles((dir, name) -> name.endsWith(".pdf")))) {
            investigatePdfFile(pdfName.getName());
        }
    }

    private static void investigatePdfFile(String pdfName) throws IOException, InterruptedException {
        Path pdfPath = PDF_PATH.resolve(pdfName);
        String pdfDir = pdfName.substring(0, pdfName.length() - 4);
        Path outputPath = PDF_PATH.resolve(pdfDir);
        createDirectories(outputPath);

        runCommands(pdfPath, outputPath);

        pdfBox(pdfPath, outputPath);

        //otherExperiments(pdfPath, outputPath);
    }

    private static void runCommands(Path pdfPath, Path outputPath) throws IOException, InterruptedException {
        String pdf = arg(pdfPath);

        runCommand(arg(EXE_PATH + "pdfdetach.exe"), "-saveall", "-o", arg(outputPath.resolve("pdfdetach")), pdf);
        runCommand(arg(EXE_PATH + "pdfdetach.exe"), "-list", "-o", arg(outputPath.resolve("embedded.txt")), pdf);
        runCommand(outputPath.resolve("fonts.txt"), arg(EXE_PATH + "pdffonts.exe"), "-loc", pdf);

        Path imagesPath = outputPath.resolve("images");
        createDirectories(imagesPath);
        Path rawImagesPath = outputPath.resolve("raw-images");
        createDirectories(rawImagesPath);
        runCommand(outputPath.resolve("images.txt"), arg(EXE_PATH + "pdfimages.exe"), "-j", "-list", pdf, arg(imagesPath.resolve("img")));
        runCommand(arg(EXE_PATH + "pdfimages.exe"), "-raw", pdf, arg(rawImagesPath.resolve("img")));
        Path finalImagesPath = imagesPath;
        Files.write(outputPath.resolve("images.txt"), Files.lines(outputPath.resolve("images.txt")).map(s -> s.replace(finalImagesPath.resolve("img").toAbsolutePath().toString(), "img")).collect(Collectors.toList()));

        runCommand(outputPath.resolve("info.txt"), arg(EXE_PATH + "pdfinfo.exe"), pdf);

        Path htmlPath = outputPath.resolve("html");
        deleteDirectoryStream(htmlPath);
        runCommand(arg(EXE_PATH + "pdftohtml.exe"), /*"-embedbackground", "-embedfonts", */"-table", pdf, arg(htmlPath));

        runCommand(arg(EXE_PATH + "pdftotext.exe"), "-layout", pdf, arg(outputPath.resolve("layout-text.txt")));
        runCommand(arg(EXE_PATH + "pdftotext.exe"), pdf, arg(outputPath.resolve("simple-text.txt")));

        runCommand(arg(EXE_PATH + "cpdf.exe"), "-draft", pdf, "AND", "-clean", "-o", arg(outputPath.resolve("draft.pdf")));
        runCommand(arg(EXE_PATH + "pdftops.exe"), arg(outputPath.resolve("draft.pdf")), arg(outputPath.resolve("draft.ps")));

        runCommand(outputPath.resolve("cpdf-fonts.txt"), arg(EXE_PATH + "cpdf.exe"), "-list-fonts", pdf);

        runCommand(outputPath.resolve("cpdf-info.txt"), arg(EXE_PATH + "cpdf.exe"), "-info", pdf);

        runCommand(outputPath.resolve("cpdf-metadata.txt"), arg(EXE_PATH + "cpdf.exe"), "-print-metadata", pdf);

        runCommand(outputPath.resolve("cpdf-page-labels.txt"), arg(EXE_PATH + "cpdf.exe"), "-print-page-labels", pdf);

        runCommand(outputPath.resolve("cpdf-annotations.txt"), arg(EXE_PATH + "cpdf.exe"), "-list-annotations", pdf);
        runCommand(outputPath.resolve("cpdf-annotations.json"), arg(EXE_PATH + "cpdf.exe"), "-list-annotations-json", pdf);

        runCommand(outputPath.resolve("cpdf-page-info.txt"), arg(EXE_PATH + "cpdf.exe"), "-page-info", pdf);
        runCommand(outputPath.resolve("cpdf-pages.txt"), arg(EXE_PATH + "cpdf.exe"), "-pages", pdf);

        runCommand(outputPath.resolve("cpdf-bookmarks.txt"), arg(EXE_PATH + "cpdf.exe"), "-list-bookmarks", pdf);
        runCommand(outputPath.resolve("cpdf-bookmarks.json"), arg(EXE_PATH + "cpdf.exe"), "-list-bookmarks-json", pdf);

        imagesPath = outputPath.resolve("cpdf-images");
        createDirectories(imagesPath);
        runCommand(arg(EXE_PATH + "cpdf.exe"), "-extract-images", pdf, "-im", IM_PATH, "-o", arg(imagesPath.resolve("img")));

        runCommand(outputPath.resolve("qpdf-attachments.txt"), arg(EXE_PATH + "qpdf.exe"), "--list-attachments", pdf);
        Files.write(outputPath.resolve("qpdf-attachments.txt"), Files.lines(outputPath.resolve("qpdf-attachments.txt")).map(s -> s.replace(pdfPath.toAbsolutePath() + " has ", "")).collect(Collectors.toList()));

        runCommand(arg(EXE_PATH + "cpdf.exe"), "-output-json", arg(outputPath.resolve("draft.pdf")), "-o", arg(outputPath.resolve("cpdf.json")));

        // это лишнее
        /*imagesPath = outputPath.resolve("pdfbox-images");
        createDirectories(imagesPath);
        runCommand("java", "-jar", arg(EXE_PATH + PDFBOX), "export:images", "-noColorConvert", "-useDirectJPEG", arg("-i=" + pdfPath.toAbsolutePath()), arg("-prefix=" + outputPath.resolve(imagesPath).resolve("img")));*/

        runCommand("java", "-jar", arg(EXE_PATH + PDFBOX), "export:text", "-html", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("pdfbox.html")));
        runCommand("java", "-jar", arg(EXE_PATH + PDFBOX), "export:text", "-rotationMagic", "-sort", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("pdfbox.txt")));
        runCommand("java", "-jar", arg(EXE_PATH + PDFBOX), "export:fdf", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("pdfbox.fdf")));
        runCommand("java", "-jar", arg(EXE_PATH + PDFBOX), "export:xfdf", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("pdfbox.xfdf")));

        // это лишнее
        /*imagesPath = outputPath.resolve("pdfbox-render");
        createDirectories(imagesPath);
        runCommand("java", "-jar", arg(EXE_PATH + PDFBOX), "render", "-format=jpg", "-i=" + pdf, arg("-prefix=" + outputPath.resolve(imagesPath).resolve("img")));*/

        /*
        cpdf -compress in.pdf -o out.pdf
        cpdf -squeeze in.pdf [-squeeze-log-to <filename>]
                [-squeeze-no-recompress] [-squeeze-no-page-data] -o out.pdf

        вероятно пригодится для сортировки содержимого
        cpdf -l in.pdf -o out.pdf
        Linearize the ﬁle in.pdf, writing to out.pdf.
        Causes generation of a linearized (web-optimized) output file.

                --compress-streams=[y
                --recompress-flate
                --compression-level=level
        When writing new streams that are compressed with /FlateDecode, use the specified compression level. The
        value of level should be a number from 1 to 9*/
    }

    @SuppressWarnings("all")
    static void deleteDirectoryStream(Path path) throws IOException {
        if (Files.exists(path) && Files.isDirectory(path))
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private static String arg(Path arg) {
        return arg(arg.toAbsolutePath().toString());
    }

    private static String arg(String arg) {
        return '"' + arg + '"';
    }

    public static void runCommand(Path output, String... args) throws IOException, InterruptedException {
        System.out.println(String.join(" ", Arrays.asList(args)));

        Process proc = new ProcessBuilder().command(args).redirectOutput(output.toFile()).start();
        BufferedReader errBR = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        BufferedReader outBR = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        int code = proc.waitFor();

        if (code > 0) {
            System.out.println("Exit code: " + code);
            System.out.println("Out message: " + errBR.lines().collect(Collectors.joining("\n")));
            System.out.println("Error message: " + outBR.lines().collect(Collectors.joining("\n")));
            System.out.println(Arrays.asList(args));
            System.exit(-1);
        }
    }

    public static void runCommand(String... args) throws IOException, InterruptedException {
        System.out.println(String.join(" ", Arrays.asList(args)));

        Process proc = new ProcessBuilder().command(args).inheritIO().start();
        BufferedReader errBR = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        BufferedReader outBR = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        int code = proc.waitFor();

        if (code > 0) {
            System.out.println("Exit code: " + code);
            System.out.println("Out message: " + errBR.lines().collect(Collectors.joining("\n")));
            System.out.println("Error message: " + outBR.lines().collect(Collectors.joining("\n")));
            System.out.println(Arrays.asList(args));
            System.exit(-1);
        }
    }
    
    private static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception ignored) {
        }
    }

    private static void addIfTrue(List<String> list, boolean condition, String value) {
        if (condition) list.add(value);
    }

    static SimpleDateFormat format = new SimpleDateFormat("EEE MMM d hh:mm:ss: yyyy");
    // Date and Time Pattern
    //Result
    //"yyyy.MM.dd G 'at' HH:mm:ss z"
    //2001.07.04 AD at 12:08:56 PDT
    //"EEE, MMM d, ''yy"
    //Wed, Jul 4, '01
    //"h:mm a"
    //12:08 PM
    //"hh 'o''clock' a, zzzz"
    //12 o'clock PM, Pacific Daylight Time
    //"K:mm a, z"
    //0:08 PM, PDT
    //"yyyyy.MMMMM.dd GGG hh:mm aaa"
    //02001.July.04 AD 12:08 PM
    //"EEE, d MMM yyyy HH:mm:ss Z"
    //Wed, 4 Jul 2001 12:08:56 -0700
    //"yyMMddHHmmssZ"
    //010704120856-0700
    //"yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    //2001-07-04T12:08:56.235-0700
    //"yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    //2001-07-04T12:08:56.235-07:00
    //"YYYY-'W'ww-u"
    //2001-W27-3

    // //CreationDate:   Thu Mar 16 02:50:06 2023

    public static void pdfBox(Path pdfPath, Path outputPath) throws IOException {
        //PDDocument document = Loader.loadPDF(new File("C:\\Users\\user\\Downloads\\Sanet.st.Retro_Gamer_UK_-_Issue_244,_2023.pdf"));
        PDDocument document = Loader.loadPDF(pdfPath.toFile());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("Version", document.getDocument().getVersion());
        output.put("Pages", document.getNumberOfPages());
        output.put("File size", Files.size(pdfPath) + " bytes");
        output.put("Encryption", document.getDocument().isEncrypted());

        AccessPermission accessPermission = document.getCurrentAccessPermission();
        List<String> permissions = new ArrayList<>();
        if (accessPermission.isOwnerPermission()) {
            permissions.add("Owner permissions (no restrictions)");
        } else {
            addIfTrue(permissions, accessPermission.canAssembleDocument(), "AssembleDocument");
            addIfTrue(permissions, accessPermission.canExtractContent(), "ExtractContent");
            addIfTrue(permissions, accessPermission.canExtractForAccessibility(), "ExtractForAccessibility");
            addIfTrue(permissions, accessPermission.canFillInForm(), "FillInForm");
            addIfTrue(permissions, accessPermission.canPrint(), "Print");
            addIfTrue(permissions, accessPermission.canModify(), "Modify");
            addIfTrue(permissions, accessPermission.canModifyAnnotations(), "ModifyAnnotations");
            addIfTrue(permissions, accessPermission.canPrintDegraded(), "PrintDegraded");
            addIfTrue(permissions, accessPermission.isOwnerPermission(), "OwnerPermission");
            addIfTrue(permissions, accessPermission.isReadOnly(), "ReadOnly");
        }

        output.put("Permissions", String.join(", ", permissions));

        if (document.getNumberOfPages() > 0) {
            output.put("Page size", String.format("%s x %s pts", document.getPage(0).getCropBox().getWidth(), document.getPage(0).getCropBox().getHeight()));
        }
        output.put("Page layout", document.getDocumentCatalog().getPageLayout().stringValue());
        output.put("Page mode", document.getDocumentCatalog().getPageMode().stringValue());

        output.put("Title", document.getDocumentInformation().getTitle());
        output.put("Author", document.getDocumentInformation().getAuthor());
        output.put("Subject", document.getDocumentInformation().getSubject());
        output.put("Keywords", document.getDocumentInformation().getKeywords());
        output.put("Creator", document.getDocumentInformation().getCreator());
        output.put("Producer", document.getDocumentInformation().getProducer());
        if (document.getDocumentInformation().getCreationDate() != null) {
            //TODO тут смещение
            output.put("Created", format.format(document.getDocumentInformation().getCreationDate().getTime()));
        }
        if (document.getDocumentInformation().getModificationDate() != null) {
            //TODO тут смещение
            output.put("Modified", format.format(document.getDocumentInformation().getModificationDate().getTime()));
        }
        output.put("Trapped", document.getDocumentInformation().getTrapped());
        output.put("Linearized", document.getDocument().getLinearizedDictionary() != null);

        //CreationDate:   Thu Mar 16 02:50:06 2023
        //ModDate:        Thu Mar 16 02:50:06 2023

        Files.write(outputPath.resolve("pdfbox-info.txt"), output.entrySet().stream().filter(e -> e.getValue() != null)
                .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.toList()));
    }

    public static void otherExperiments(Path pdfPath, Path outputPath) throws IOException {
        //PDDocument document = Loader.loadPDF(new File("C:\\Users\\user\\Downloads\\Sanet.st.Retro_Gamer_UK_-_Issue_244,_2023.pdf"));
        PDDocument document = Loader.loadPDF(pdfPath.toFile());

        //System.out.println(document.getDocumentInformation()); // PDDocumentInformation

        System.out.println("getCOSObject: " + document.getDocumentInformation().getCOSObject());
        document.getDocumentInformation().getCOSObject().entrySet().forEach(e -> System.out.println(e.getKey() + ": " + document.getDocumentInformation().getCustomMetadataValue(e.getKey().getName())));
        document.getDocumentInformation().getCOSObject().entrySet().forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));

        System.out.println("getMetadataKeys: " + document.getDocumentInformation().getMetadataKeys());
        document.getDocumentInformation().getMetadataKeys().forEach(key -> System.out.println(key + ": " + document.getDocumentInformation().getCustomMetadataValue(key)));

        System.out.println(document.getDocument()); // COSDocument
        System.out.println(document.getDocument().getVersion()); // 1.4
        //System.out.println(document.getDocument().getObjectsByType());
        System.out.println(document.getDocument().getDocumentID()); // COSArray{[COSString{$]˛–ÇÏŁłé©⁄Ð¤}, COSString{Ž�3é#Ii©−êkàÆHâ}]}
        System.out.println(document.getDocument().getHighestXRefObjectNumber()); // 6336
        System.out.println(document.getDocument().isXRefStream()); // false
        //System.out.println(document.getDocument().accept());
        System.out.println(document.getDocument().createCOSStream()); // COSDictionary{Create InputStream called without data being written before to stream.}
        System.out.println(document.getDocument().getEncryptionDictionary()); // null
        System.out.println(document.getDocument().getLinearizedDictionary()); // null
        //System.out.println(document.getDocument().getObjectFromPool());
        System.out.println(document.getDocument().getStartXref()); // 88493080
        //System.out.println(document.getDocument().getTrailer()); // COSDictionary //TODO
        //System.out.println(document.getDocument().getXrefTable()); // Map<COSObjectKey, Long> TODO
        System.out.println(document.getDocument().isClosed()); // false
        System.out.println(document.getDocument().isDecrypted()); // true
        System.out.println(document.getDocument().isEncrypted()); // false
        System.out.println(document.getDocument().getCOSObject()); // COSDocument
        System.out.println(document.getDocument().getKey()); // null
        System.out.println(document.getDocument().isDirect());

        System.out.println("!!!!!!!!!!!!!!!");

        System.out.println(document.getDocumentCatalog()); // PDDocumentCatalog
        System.out.println(document.getDocumentCatalog().getDocumentOutline()); // null
        System.out.println(document.getDocumentCatalog().getAcroForm()); // null
        //System.out.println(document.getDocumentCatalog().getCOSObject()); // COSDictionary //TODO
        //System.out.println(document.getDocumentCatalog().findNamedDestinationPage());
        System.out.println(document.getDocumentCatalog().getActions()); // PDDocumentCatalogAdditionalActions
        System.out.println(document.getDocumentCatalog().getDests()); // null
        System.out.println(document.getDocumentCatalog().getLanguage()); // null
        System.out.println(document.getDocumentCatalog().getMarkInfo()); // null
        System.out.println(document.getDocumentCatalog().getMetadata()); // PDMetadata
        System.out.println(document.getDocumentCatalog().getNames()); // null
        System.out.println(document.getDocumentCatalog().getOCProperties()); // null
        System.out.println(document.getDocumentCatalog().getOpenAction()); // null
        System.out.println(document.getDocumentCatalog().getOutputIntents()); // []
        System.out.println(document.getDocumentCatalog().getPageLabels()); // null
        System.out.println(document.getDocumentCatalog().getPageLayout()); // SINGLE_PAGE
        System.out.println(document.getDocumentCatalog().getPageMode()); // USE_NONE
        System.out.println(document.getDocumentCatalog().getPages()); // PDPageTree
        System.out.println(document.getDocumentCatalog().getStructureTreeRoot()); // null
        System.out.println(document.getDocumentCatalog().getThreads()); // COSArrayList{COSArray{[]}}
        System.out.println(document.getDocumentCatalog().getURI()); // null
        System.out.println(document.getDocumentCatalog().getVersion()); // null
        System.out.println(document.getDocumentCatalog().getViewerPreferences()); // null

        System.out.println(document.getEncryption()); // null
        System.out.println(document.getDocumentId()); // null

        System.out.println(document.getCurrentAccessPermission()); // AccessPermission

        System.out.println(document.getResourceCache()); // DefaultResourceCache

        System.out.println(document.getLastSignatureDictionary()); // null
        System.out.println(document.getSignatureDictionaries()); // []
        System.out.println(document.getSignatureFields()); // []

        System.out.println(document.getPages()); // PDPageTree

        System.out.println(document.getPage(0)); // PDPage
        System.out.println(document.getPage(0).getAnnotations()); // COSArrayList{COSArray{[]}}
        System.out.println(document.getPage(0).getMatrix()); // [1.0,0.0,0.0,1.0,0.0,0.0]
        System.out.println(document.getPage(0).getMediaBox()); // [0.0,0.0,651.97,841.89]
        System.out.println(document.getPage(0).getArtBox()); // [0.0,0.0,651.97,841.89]
        System.out.println(document.getPage(0).getBBox()); // [0.0,0.0,651.97,841.89]
        System.out.println(document.getPage(0).getBleedBox()); // [0.0,0.0,651.97,841.89]
        System.out.println(document.getPage(0).getCropBox()); // [0.0,0.0,651.97,841.89]
        System.out.println(document.getPage(0).getTrimBox()); // [0.0,0.0,651.969,841.89]
        System.out.println(document.getPage(0).getActions()); // PDPageAdditionalActions
        System.out.println(document.getPage(0).getContents()); // COSInputStream
        System.out.println(document.getPage(0).getContentsForRandomAccess()); // RandomAccessReadBuffer
        System.out.println(document.getPage(0).getContentStreams()); // java.util.ArrayList$Itr@532760d8
        //System.out.println(document.getPage(0).getCOSObject());
        System.out.println(document.getPage(0).getMetadata()); // null
        System.out.println(document.getPage(0).getResourceCache()); // DefaultResourceCache
        System.out.println(document.getPage(0).getRotation()); // 0
        System.out.println(document.getPage(0).getStructParents()); // -1
        System.out.println(document.getPage(0).getThreadBeads()); // COSArrayList{COSArray{[]}}
        System.out.println(document.getPage(0).getTransition()); // null
        System.out.println(document.getPage(0).getUserUnit()); // 1.0
        System.out.println(document.getPage(0).getViewports()); // null
        System.out.println(document.getPage(0).hasContents()); // true*//*

        System.out.println(document.getPage(0).getResources()); // PDResources

        int index = 0;
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            System.out.println("=========================== Page: " + index++);

            System.out.println("--------------------------- XObject:");
            for (COSName cosName : resources.getXObjectNames()) {
                if (cosName.getKey() != null) {
                    System.out.println();
                }
                boolean isImage = resources.isImageXObject(cosName);
                System.out.printf("%s: isImage: %s; hasColorSpace: %s%n", cosName.getName(), isImage, resources.hasColorSpace(cosName));
                PDXObject object = resources.getXObject(cosName);
                System.out.println(object);
                System.out.println(object.getCOSObject());
                //System.out.println(object.getCOSObject().getCOSObject());

                System.out.println(object.getCOSObject().getKey()); // null
                //System.out.println(object.getCOSObject().getCOSName());
                System.out.println(object.getCOSObject().getFilters()); // COSName{DCTDecode}
                System.out.println(object.getCOSObject().getLength());
                System.out.println(object.getCOSObject().hasData());
                if (!isImage) {
                    //System.out.println(object.getCOSObject().toTextString());
                }
                //System.out.println(object.getStream());
                System.out.println(object.getStream().getDecodedStreamLength());
                System.out.println(object.getStream().getDecodeParms());
                System.out.println(object.getStream().getFile());
                System.out.println(object.getStream().getFileDecodeParams());
                System.out.println(object.getStream().getFileFilters());
                System.out.println(object.getStream().getFilters());
                System.out.println(object.getStream().getLength());
                System.out.println(object.getStream().getMetadata());
                //System.out.println(object.getStream().toByteArray());
            }

            System.out.println("--------------------------- ExtGStates:");
            for (COSName cosName : resources.getExtGStateNames()) {
                System.out.println(resources.getExtGState(cosName));
            }

            System.out.println("--------------------------- ColorSpaces:");
            for (COSName cosName : resources.getColorSpaceNames()) {
                System.out.println(resources.getColorSpace(cosName));
            }

            System.out.println("--------------------------- Fonts:");
            for (COSName cosName : resources.getFontNames()) {
                System.out.println(resources.getFont(cosName));
            }

            System.out.println("--------------------------- Patterns:");
            for (COSName cosName : resources.getPatternNames()) {
                System.out.println(resources.getPattern(cosName));
            }

            System.out.println("--------------------------- Properties:");
            for (COSName cosName : resources.getPropertiesNames()) {
                System.out.println(resources.getProperties(cosName));
            }

            System.out.println("--------------------------- Shadings:");
            for (COSName cosName : resources.getShadingNames()) {
                System.out.println(resources.getShading(cosName));
            }

            System.out.println();
        }
    }
}
