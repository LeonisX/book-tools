package md.leonis;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class PdfAnalyzer {

    //todo
    // сжимать - qpdf, pdfcpu - надо выбрать лучший, но чтобы не ломал
    // установить метадату: cpdf

    private static final Path EXE_PATH = Paths.get(".").resolve("commands");

    private static final String CPDF = arg(EXE_PATH.resolve("cpdf").resolve("cpdf.exe"));
    private static final String QPDF = arg(EXE_PATH.resolve("qpdf").resolve("qpdf.exe"));
    private static final String PDFCPU = arg(EXE_PATH.resolve("pdfcpu").resolve("pdfcpu.exe"));
    private static final String PDFBOX = arg(EXE_PATH.resolve("pdfbox").resolve("pdfbox-app-3.0.0-alpha3.jar"));
    // XPDF
    private static final String PDFDETACH = arg(EXE_PATH.resolve("xpdf").resolve("pdfdetach.exe"));
    private static final String PDFFONTS = arg(EXE_PATH.resolve("xpdf").resolve("pdffonts.exe"));
    private static final String PDFIMAGES = arg(EXE_PATH.resolve("xpdf").resolve("pdfimages.exe"));
    private static final String PDFINFO = arg(EXE_PATH.resolve("xpdf").resolve("pdfinfo.exe"));
    private static final String PDFTOHTML = arg(EXE_PATH.resolve("xpdf").resolve("pdftohtml.exe"));
    private static final String PDFTOPS = arg(EXE_PATH.resolve("xpdf").resolve("pdftops.exe"));
    private static final String PDFTOTEXT = arg(EXE_PATH.resolve("xpdf").resolve("pdftotext.exe"));
    // POPPLER
    private static final String PDFIMAGES_POP = arg(EXE_PATH.resolve("poppler").resolve("pdfimages.exe"));

    private static final Path PDF_PATH = Paths.get("G:\\transcend\\Emu\\4tivo\\Magazines\\Eng\\Retro Gamer\\test");
    private static final String IM_PATH = "C:\\Program Files\\ImageMagick-7.1.1-Q16-HDRI\\magick.exe";

    public static void main(String[] args) throws IOException, InterruptedException {
        pdfToDir();

        for (File pdfName : Objects.requireNonNull(PDF_PATH.toFile().listFiles((dir, name) -> name.endsWith(".pdf")))) {
            //validatePdfFile(pdfName.getName()); //todo parallel execution, это валидация, стоит выполнять раз для всей пачки
            optimizePdfFile(PDF_PATH, pdfName.getName());
            investigatePdfFile(PDF_PATH, pdfName.getName());
        }
        //investigatePdfFile(PDF_PATH, "Retro.Gamer.Annual.Volume.8.2022-MagDownload.org-compressed-qpdf.pdf");

        //тот код разбирает оптимизированные. надо ещё оптимизированные акробатом смотреть
        for (File pdfName : Objects.requireNonNull(PDF_PATH.resolve("optimized").toFile().listFiles((dir, name) -> name.endsWith(".pdf")))) {
            optimizePdfFile(PDF_PATH.resolve("optimized"), pdfName.getName());
            investigatePdfFile(PDF_PATH.resolve("optimized"), pdfName.getName());
        }
    }

    private static void validatePdfFile(String pdfName) throws IOException, InterruptedException {
        Path outputPath = PDF_PATH.resolve("validations");
        createDirectories(outputPath);
        Path pdfPath = PDF_PATH.resolve(pdfName);
        String pdf = arg(pdfPath);

        String pdfFile = pdfName.substring(0, pdfName.length() - 4);
        String pdfCpu = pdfFile + "-pdfcpu.txt";
        String qpdf = pdfFile + "-qpdf.txt";
        runCommand(outputPath.resolve(pdfCpu), PDFCPU, "validate", pdf);
        runCommand(outputPath.resolve(qpdf), QPDF, "--check", "--with-images", pdf);

        sanitizeFile(outputPath.resolve(pdfCpu), pdfPath);
        sanitizeFile(outputPath.resolve(qpdf), pdfPath);
    }

    private static void optimizePdfFile(Path inPdfPath, String pdfName) throws IOException, InterruptedException {
        Path outputPath = PDF_PATH.resolve("optimized");
        createDirectories(outputPath);
        Path pdfPath = inPdfPath.resolve(pdfName);
        String pdf = arg(pdfPath);
        String pdfFile = pdfName.substring(0, pdfName.length() - 4);

        // если файл с неверными полями то валится.
        runCommand(PDFCPU, "optimize", "-stats", arg(outputPath.resolve(pdfFile + "-stats-pdfcpu.csv")), pdf, arg(outputPath.resolve(pdfFile + "-optimized-pdfcpu.pdf")));

        //runCommand(CPDF, "-compress", pdf, "-o", arg(outputPath.resolve(pdfFile + "-compressed-cpdf.pdf")));
        //runCommand(CPDF, "-squeeze", pdf, "-o", arg(outputPath.resolve(pdfFile + "-squeezeed-cpdf.pdf"))); // жмёт хорошо, но может уменьшать картинки

        //todo выключить оптимизацию картинок?
        runCommand(QPDF, "--stream-data=compress", "--recompress-flate", "--compression-level=9",
                "--normalize-content=n", "--object-streams=generate", "--optimize-images", "--oi-min-width=0", "--oi-min-height=0",
                "--oi-min-area=0", "--min-version=1.7", pdf, arg(outputPath.resolve(pdfFile + "-compressed-qpdf.pdf")));

        /*runCommand(QPDF, "--stream-data=compress", "--recompress-flate", "--compression-level=9",
                "--normalize-content=n", "--object-streams=generate", "--optimize-images", "--oi-min-width=0", "--oi-min-height=0",
                "--oi-min-area=0", "--min-version=1.7", "--linearize", pdf, arg(outputPath.resolve(pdfFile + "-compressed-linearized-qpdf.pdf")));*/

        // --externalize-inline-images -ii-min-bytes=0
        //runCommand(QPDF, "--linearize", pdf, arg(outputPath.resolve(pdfFile + "-linearized-qpdf.pdf")));

        // mutool convert генерирует раздутые файлы. Не годится.

        //todo
        // cpdf set metadata

        //TIFF
        Path tiffOutputPath = PDF_PATH.resolve("tiff").resolve(pdfFile);
        createDirectories(tiffOutputPath);
        runCommand(PDFIMAGES_POP, "-tiff", pdf, arg(tiffOutputPath.resolve("img")));
    }

    private static void investigatePdfFile(Path inPdfPath, String pdfName) throws IOException, InterruptedException {
        Path pdfPath = inPdfPath.resolve(pdfName);
        String pdfDir = pdfName.substring(0, pdfName.length() - 4);
        Path outputPath = PDF_PATH.resolve(pdfDir);
        createDirectories(outputPath);

        pdfBoxInfo(pdfPath, outputPath);

        runCommands(pdfPath, outputPath);

        //otherExperiments(pdfPath, outputPath);
    }

    private static void runCommands(Path sourcePdfPath, Path outputPath) throws IOException, InterruptedException {
        Path pdfPath = outputPath.resolve("pdf.pdf");
        Files.deleteIfExists(pdfPath);
        Files.copy(sourcePdfPath, pdfPath);
        String pdf = arg(pdfPath);

        // Info
        runCommand(outputPath.resolve("info.txt"), PDFINFO, pdf);
        runCommand(outputPath.resolve("info-cpdf.txt"), CPDF, "-info", pdf);
        runCommand(outputPath.resolve("info-pdfcpu.txt"), PDFCPU, "info", pdf);

        runCommand(outputPath.resolve("metadata-cpdf.txt"), CPDF, "-print-metadata", pdf);
        //runCommand(outputPath.resolve("annotations-cpdf.txt"), CPDF, "-list-annotations", pdf);
        //runCommand(outputPath.resolve("annotations-cpdf.json"), CPDF, "-list-annotations-json", pdf);
        runCommand(outputPath.resolve("page-info-cpdf.txt"), CPDF, "-page-info", pdf);
        //runCommand(outputPath.resolve("pages-cpdf.txt"), CPDF, "-pages", pdf);
        //runCommand(outputPath.resolve("page-labels-cpdf.txt"), CPDF, "-print-page-labels", pdf);
        runCommand(outputPath.resolve("bookmarks-cpdf.txt"), CPDF, "-list-bookmarks", pdf);
        runCommand(outputPath.resolve("bookmarks-cpdf.json"), CPDF, "-list-bookmarks-json", pdf);
        runCommand(outputPath.resolve("annotations-pdfcpu.txt"), PDFCPU, "annotations", "list", pdf);
        runCommand(outputPath.resolve("box-pdfcpu.txt"), PDFCPU, "box", "list", pdf);
        //runCommand(outputPath.resolve("permissions-pdfcpu.txt"), PDFCPU, "permissions", "list", pdf);
        runCommand(outputPath.resolve("form-pdfcpu.txt"), PDFCPU, "form", "list", pdf);
        runCommand(outputPath.resolve("keywords-pdfcpu.txt"), PDFCPU, "keywords", "list", pdf);
        runCommand(outputPath.resolve("portfolio-pdfcpu.txt"), PDFCPU, "portfolio", "list", pdf);
        runCommand(outputPath.resolve("properties-pdfcpu.txt"), PDFCPU, "properties", "list", pdf);

        sanitizeFile(outputPath.resolve("box-pdfcpu.txt"), pdfPath);

        createDirectories(outputPath.resolve("content-pdfcpu"));
        createDirectories(outputPath.resolve("meta-pdfcpu"));
        runCommand(PDFCPU, "extract", "-mode", "content", pdf, arg(outputPath.resolve("content-pdfcpu")));
        runCommand(PDFCPU, "extract", "-mode", "meta", pdf, arg(outputPath.resolve("meta-pdfcpu")));

        runCommand(outputPath.resolve("xrefs-qpdf.txt"), QPDF, "--show-xref", pdf);

        // List fonts
        runCommand(outputPath.resolve("fonts.txt"), PDFFONTS, "-loc", pdf);
        runCommand(outputPath.resolve("fonts-cpdf.txt"), CPDF, "-list-fonts", pdf);

        // Attachments
        runCommand(PDFDETACH, "-saveall", "-o", arg(outputPath.resolve("pdfdetach")), pdf);
        runCommand(PDFDETACH, "-list", "-o", arg(outputPath.resolve("embedded.txt")), pdf);
        runCommand(outputPath.resolve("attachments-qpdf.txt"), QPDF, "--list-attachments", pdf);
        sanitizeFile(outputPath.resolve("attachments-qpdf.txt"), pdfPath);

        createDirectories(outputPath.resolve("attachments-pdfcpu"));
        runCommand(outputPath.resolve("attachments-pdfcpu.txt"), PDFCPU, "attachments", "list", pdf);
        runCommand(PDFCPU, "attachments", "extract", pdf, arg(outputPath.resolve("attachments-pdfcpu")));

        // Images
        Path imagesPath = outputPath.resolve("images");
        createDirectories(imagesPath);
        Path rawImagesPath = outputPath.resolve("images-raw");
        createDirectories(rawImagesPath);
        runCommand(outputPath.resolve("images.txt"), PDFIMAGES, "-j", "-list", pdf, arg(imagesPath.resolve("img")));
        runCommand(PDFIMAGES, "-raw", pdf, arg(rawImagesPath.resolve("img")));
        sanitizeFile(outputPath.resolve("images.txt"), imagesPath);

        // не всегда вытаскивает все картинки
        /*imagesPath = outputPath.resolve("images-cpdf");
        createDirectories(imagesPath);
        runCommand(CPDF, "-extract-images", pdf, "-im", IM_PATH, "-o", arg(imagesPath.resolve("img")));*/

        // это лишнее
        /*imagesPath = outputPath.resolve("images-pdfbox");
        createDirectories(imagesPath);
        runCommand("java", "-jar", PDFBOX, "export:images", "-noColorConvert", "-useDirectJPEG", arg("-i=" + pdfPath.toAbsolutePath()), arg("-prefix=" + outputPath.resolve(imagesPath).resolve("img")));*/

        imagesPath = outputPath.resolve("images-pdfcpu");
        createDirectories(imagesPath);
        runCommand(outputPath.resolve("images-pdfcpu.txt"), PDFCPU, "images", "list", pdf);
        runCommand(PDFCPU, "extract", "-mode", "image", pdf, arg(imagesPath));
        sanitizeFile(outputPath.resolve("images-pdfcpu.txt"), pdfPath);

        // Html, fonts
        Path htmlPath = outputPath.resolve("html");
        deleteDirectoryStream(htmlPath);
        runCommand(PDFTOHTML, "-table", pdf, arg(htmlPath));
        //runCommand(PDFTOHTML, "-embedbackground", "-embedfonts", "-table", pdf, arg(htmlPath));

        runCommand("java", "-jar", PDFBOX, "export:text", "-html", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("html-pdfbox.html")));

        Path fontsPath = outputPath.resolve("fonts-pdfcpu");
        createDirectories(fontsPath);
        runCommand(PDFCPU, "extract", "-mode", "font", pdf, arg(fontsPath));

        // Text
        runCommand(PDFTOTEXT, "-layout", "-enc", "UTF-8", pdf, arg(outputPath.resolve("text-layout.txt")));
        runCommand(PDFTOTEXT, "-enc", "UTF-8", pdf, arg(outputPath.resolve("text-simple.txt")));

        // PostScript, structure
        /*runCommand(CPDF, "-draft", pdf, "AND", "-clean", "-o", arg(outputPath.resolve("pdf-draft.pdf")));
        runCommand(PDFTOPS, arg(outputPath.resolve("pdf-draft.pdf")), arg(outputPath.resolve("pdf-draft.ps")));
        runCommand(CPDF, "-output-json", arg(outputPath.resolve("pdf-draft.pdf")), "-o", arg(outputPath.resolve("pdf-cpdf.json")));*/

        runCommand("java", "-jar", PDFBOX, "export:text", "-rotationMagic", "-sort", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("text-pdfbox.txt")));
        /*runCommand("java", "-jar", PDFBOX, "export:fdf", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("pdf.fdf")));
        runCommand("java", "-jar", PDFBOX, "export:xfdf", arg("-i=" + pdfPath.toAbsolutePath()), arg("-o=" + outputPath.resolve("pdf.xfdf")));*/

        /*runCommand(QPDF, "--remove-restrictions", "--decrypt", "--object-streams=disable",
                "--deterministic-id", "--qdf", "--no-original-object-ids", "--coalesce-contents", "--normalize-content=y", pdf, arg(outputPath.resolve("pdf.qdf")));
        runCommand(QPDF, "--remove-restrictions", "--decrypt", "--object-streams=disable",
                "--deterministic-id", "--json-output", "--no-original-object-ids", "--coalesce-contents", "--normalize-content=y", pdf, arg(outputPath.resolve("pdf.json")));*/

        // Render

        // это лишнее
        /*imagesPath = outputPath.resolve("pdfbox-render");
        createDirectories(imagesPath);
        runCommand("java", "-jar", PDFBOX, "render", "-format=jpg", "-i=" + pdf, arg("-prefix=" + outputPath.resolve(imagesPath).resolve("img")));*/

        // mutool draw [options] file [pages]
        // На самом деле, mutool - достаточно бестолковая утилита.
        // Позволяет извлекать все шрифты, но в текущий каталог и вместе с картинками
        // Очень мало настроек а те что есть - бестолковые.
        // clean раздувает файл ещё больше.
        // Преобразование в PDF, XPS, CBZ, unprotected EPUB, FB2, etc.
        // mutool trace - показывает как рендерится страница
        // так же есть другие команды https://mupdf.com/docs/mutool.html


        Files.delete(pdfPath);

        for (File file : Objects.requireNonNull(outputPath.toFile().listFiles())) {
            if (file.isFile() && file.length() == 0)
                file.delete();
        }

        for (File file : Objects.requireNonNull(outputPath.toFile().listFiles())) {
            if (file.isDirectory() && file.listFiles().length == 0)
                file.delete();
        }
    }

    private static void sanitizeFile(Path path, Path pdfPath) throws IOException {
        Files.write(path, Files.lines(path).map(s -> s.replace(pdfPath.toAbsolutePath().toString(), "PDF file")).collect(Collectors.toList()));
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

    private static void pdfToDir() {
        //Path cat = Paths.get("G:\\transcend\\Emu\\4tivo\\Magazines\\Eng\\Retro Gamer\\1. Retro Gamer (UK) (2004-Present)");
        //Path cat = Paths.get("G:\\transcend\\Emu\\4tivo\\Magazines\\Eng\\Retro Gamer\\3. Retro Gamer Annual (UK) (2015-Present)");
        Path cat = Paths.get("G:\\transcend\\Emu\\4tivo\\Magazines\\Eng\\Retro Gamer\\4. Retro Gamer Special (UK)");
        for (File pdfFile : Objects.requireNonNull(cat.toFile().listFiles((dir, name) -> name.endsWith(".pdf")))) {
            String name = pdfFile.getName();
            name = name.substring(0, name.length() - 4);
            createDirectories(cat.resolve(name));
            pdfFile.renameTo(cat.resolve(name).resolve(pdfFile.getName()).toFile());
        }
    }

    public static void runCommand(Path output, String... args) throws IOException, InterruptedException {
        System.out.println(String.join(" ", Arrays.asList(args)));

        Process proc = new ProcessBuilder().command(args).redirectOutput(output.toFile()).redirectError(output.toFile()).start();
        BufferedReader errBR = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        BufferedReader outBR = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        int code = proc.waitFor();

        if (code > 0) {
            System.out.println("Exit code: " + code);
            System.out.println("Out message: " + errBR.lines().collect(Collectors.joining("\n")));
            System.out.println("Error message: " + outBR.lines().collect(Collectors.joining("\n")));
            System.out.println(Arrays.asList(args));
            //System.exit(-1);
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
            //System.exit(-1);
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

    static SimpleDateFormat format = new SimpleDateFormat("EEE MMM d hh:mm:ss: yyyy", Locale.ENGLISH);

    public static void pdfBoxInfo(Path pdfPath, Path outputPath) {
        try {
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
                permissions.add("Full access");
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
                PDPage page = document.getPage(0);
                //TODO тут на самом деле берётся от всех боксов и выводятся все габариты какие есть
                output.put("Page size", String.format("%s x %s pts (rotated %s degrees)", page.getMediaBox().getWidth(), page.getMediaBox().getHeight(), page.getRotation()));
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
                output.put("Created", format.format(document.getDocumentInformation().getCreationDate().getTime())); // Thu Mar 16 02:50:06 2023
            }
            if (document.getDocumentInformation().getModificationDate() != null) {
                //TODO тут смещение
                output.put("Modified", format.format(document.getDocumentInformation().getModificationDate().getTime()));
            }
            output.put("Trapped", document.getDocumentInformation().getTrapped());
            output.put("Linearized", document.getDocument().getLinearizedDictionary() != null);

            String text = null;
            try {
                PDDocumentCatalog catalog = document.getDocumentCatalog();
                PDMetadata metadata = catalog.getMetadata();
                if (metadata != null) {
                    InputStream xmlInputStream = metadata.exportXMPMetadata();

                    text = new BufferedReader(
                            new InputStreamReader(xmlInputStream, StandardCharsets.UTF_8))
                            .lines().map(s -> s.replace("xap:", "xmp:"))
                            .map(s -> s.replace("xmp:", ""))
                            .collect(Collectors.joining("\n"));

                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    InputSource src = new InputSource();
                    src.setCharacterStream(new StringReader(text));
                    Document doc = builder.parse(src);
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    NodeList nodeList = (NodeList) xPath.compile("//CreateDate").evaluate(doc, XPathConstants.NODESET);
                    if (nodeList.getLength() > 0) {
                        output.put("XMP xmp:CreateDate", nodeList.item(0).getTextContent());
                    }
                    nodeList = (NodeList) xPath.compile("//MetadataDate").evaluate(doc, XPathConstants.NODESET);
                    if (nodeList.getLength() > 0) {
                        output.put("XMP xmp:MetadataDate", nodeList.item(0).getTextContent());
                    }
                    nodeList = (NodeList) xPath.compile("//ModifyDate").evaluate(doc, XPathConstants.NODESET);
                    if (nodeList.getLength() > 0) {
                        output.put("XMP xmp:ModifyDate", nodeList.item(0).getTextContent());
                    }

                    // TODO
                    //XMP pdf:Producer: Acrobat Distiller 9.4.2 (Windows)
                    //XMP xmp:CreateDate: 2020-10-12T19:19:51+03:00
                    //XMP xmp:CreatorTool: PScript5.dll Version 5.2.2
                    //XMP xmp:MetadataDate: 2020-10-12T19:52:53+03:00
                    //XMP xmp:ModifyDate: 2020-10-12T19:52:53+03:00
                    //XMP dc:title: puteshestvie-111761.indd
                    //XMP dc:creator: Guk.AL

/*                <?xpacket begin="ï»¿" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 4.2.1-c043 52.372728, 2009/01/18-15:08:04        ">
   <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
      <rdf:Description rdf:about=""
                xmlns:dc="http://purl.org/dc/elements/1.1/">
         <dc:format>application/pdf</dc:format>
         <dc:title>
            <rdf:Alt>
               <rdf:li xml:lang="x-default">puteshestvie-111761.indd</rdf:li>
            </rdf:Alt>
         </dc:title>
         <dc:creator>
            <rdf:Seq>
               <rdf:li>Guk.AL</rdf:li>
            </rdf:Seq>
         </dc:creator>
      </rdf:Description>
      <rdf:Description rdf:about=""
                xmlns:xmp="http://ns.adobe.com/xap/1.0/">
         <xmp:CreateDate>2020-10-12T19:19:51+03:00</xmp:CreateDate>
         <xmp:CreatorTool>PScript5.dll Version 5.2.2</xmp:CreatorTool>
         <xmp:ModifyDate>2020-10-12T19:52:53+03:00</xmp:ModifyDate>
         <xmp:MetadataDate>2020-10-12T19:52:53+03:00</xmp:MetadataDate>
      </rdf:Description>
      <rdf:Description rdf:about=""
                xmlns:pdf="http://ns.adobe.com/pdf/1.3/">
         <pdf:Producer>Acrobat Distiller 9.4.2 (Windows)</pdf:Producer>
      </rdf:Description>
      <rdf:Description rdf:about=""
                xmlns:xmpMM="http://ns.adobe.com/xap/1.0/mm/">
         <xmpMM:DocumentID>uuid:10bd32a6-d615-44db-83f0-390aadb32d83</xmpMM:DocumentID>
         <xmpMM:InstanceID>uuid:63405b54-237b-475a-b61e-c627da15260e</xmpMM:InstanceID>
      </rdf:Description>
   </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>*/


                    //TODO Trapped
                    // XMP pdf:Trapped: False

                /*<?xpacket begin="ï»¿" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 4.2.1-c043 52.372728, 2009/01/18-15:08:04        ">
   <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
      <rdf:Description rdf:about=""
            xmlns:xmp="http://ns.adobe.com/xap/1.0/">
         <xmp:CreateDate>2021-05-31T13:14:19+03:00</xmp:CreateDate>
         <xmp:MetadataDate>2021-05-31T13:55:33+03:00</xmp:MetadataDate>
         <xmp:ModifyDate>2021-05-31T13:55:33+03:00</xmp:ModifyDate>
         <xmp:CreatorTool>Adobe InDesign CS6 (Windows)</xmp:CreatorTool>
      </rdf:Description>
      <rdf:Description rdf:about=""
            xmlns:xmpMM="http://ns.adobe.com/xap/1.0/mm/"
            xmlns:stRef="http://ns.adobe.com/xap/1.0/sType/ResourceRef#"
            xmlns:stEvt="http://ns.adobe.com/xap/1.0/sType/ResourceEvent#">
         <xmpMM:InstanceID>uuid:f5e0f6b4-fb8c-4830-80bc-b36a0a4fe024</xmpMM:InstanceID>
         <xmpMM:OriginalDocumentID>xmp.did:D4FA0FF00D2068118083C82303B9D5EE</xmpMM:OriginalDocumentID>
         <xmpMM:DocumentID>xmp.id:87EF073EF4C1EB11A82F93703F0DD0C2</xmpMM:DocumentID>
         <xmpMM:RenditionClass>proof:pdf</xmpMM:RenditionClass>
         <xmpMM:DerivedFrom rdf:parseType="Resource">
            <stRef:instanceID>xmp.iid:85EF073EF4C1EB11A82F93703F0DD0C2</stRef:instanceID>
            <stRef:documentID>xmp.did:7870EB1959BDEB11BB76DED733636316</stRef:documentID>
            <stRef:originalDocumentID>xmp.did:D4FA0FF00D2068118083C82303B9D5EE</stRef:originalDocumentID>
            <stRef:renditionClass>default</stRef:renditionClass>
         </xmpMM:DerivedFrom>
         <xmpMM:History>
            <rdf:Seq>
               <rdf:li rdf:parseType="Resource">
                  <stEvt:action>converted</stEvt:action>
                  <stEvt:parameters>from application/x-indesign to application/pdf</stEvt:parameters>
                  <stEvt:softwareAgent>Adobe InDesign CS6 (Windows)</stEvt:softwareAgent>
                  <stEvt:changed>/</stEvt:changed>
                  <stEvt:when>2021-05-31T13:14:19+03:00</stEvt:when>
               </rdf:li>
            </rdf:Seq>
         </xmpMM:History>
      </rdf:Description>
      <rdf:Description rdf:about=""
            xmlns:dc="http://purl.org/dc/elements/1.1/">
         <dc:format>application/pdf</dc:format>
      </rdf:Description>
      <rdf:Description rdf:about=""
            xmlns:pdf="http://ns.adobe.com/pdf/1.3/">
         <pdf:Producer>Adobe PDF Library 10.0.1</pdf:Producer>
         <pdf:Trapped>False</pdf:Trapped>
      </rdf:Description>
   </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>*/
                }
            } catch (Exception e) {
                System.out.println(text);
                e.printStackTrace();
            }

            Files.write(outputPath.resolve("info-pdfbox.txt"), output.entrySet().stream().filter(e -> e.getValue() != null)
                    .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.toList()));
        } catch (Exception e) {
            System.out.println("Error processing: " + pdfPath);
            e.printStackTrace();
        }
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
