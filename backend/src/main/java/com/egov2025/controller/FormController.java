package com.egov2025.controller;

import com.egov2025.model.Cupon;
import com.egov2025.model.Plata;
import com.egov2025.repository.CuponRepository;
import com.egov2025.repository.PlataRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.itextpdf.text.pdf.ColumnText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FormController {

    @Autowired
    private PlataRepository repo;

    @Autowired
    private CuponRepository cuponRepo;

    // ✅ Endpoint pentru trimiterea formularului
    @PostMapping("/submit")
    public ResponseEntity<InputStreamResource> submit(@RequestBody Map<String, Object> payload)
            throws IOException, DocumentException {

        String email = (String) payload.get("email");
        String telefon = (String) payload.get("telefon");
        String inmatriculare = (String) payload.get("inmatriculare");
        int durata = Integer.parseInt(payload.get("durata").toString());

        // prețuri pentru durate diferite
        var preturi = Map.of(1, 5.0, 2, 10.0, 5, 20.0, 24, 50.0);
        double suma = preturi.getOrDefault(durata, 0.0);
        double tva = suma * 0.19;
        double total = suma + tva;

        // preluăm reducerea și codul de reducere din payload
        double reducere = payload.get("reducere") != null ? Double.parseDouble(payload.get("reducere").toString()) : 0;
        String codReducere = payload.get("codReducere") != null ? payload.get("codReducere").toString() : null;

        // aplicăm reducerea dacă există
        if (reducere > 0) {
            total = total - (total * reducere / 100);
        }

        // 🔹 Construim XML-ul
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<plata>\n");
        xmlBuilder.append("  <email>").append(email).append("</email>\n");
        xmlBuilder.append("  <telefon>").append(telefon).append("</telefon>\n");
        xmlBuilder.append("  <inmatriculare>").append(inmatriculare).append("</inmatriculare>\n");
        xmlBuilder.append("  <durata>").append(durata).append("</durata>\n");
        xmlBuilder.append("  <suma>").append(String.format("%.2f", suma)).append("</suma>\n");
        xmlBuilder.append("  <tva>").append(String.format("%.2f", tva)).append("</tva>\n");
        xmlBuilder.append("  <total>").append(String.format("%.2f", total)).append("</total>\n");
        xmlBuilder.append("  <reducere>").append(String.format("%.0f", reducere)).append("</reducere>\n");
        xmlBuilder.append("  <codReducere>").append(codReducere != null ? codReducere : "").append("</codReducere>\n");
        xmlBuilder.append("</plata>");
        String xmlContent = xmlBuilder.toString();

        // 🗂️ Salvăm XML-ul local într-un folder sigur
        String basePath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "xml";
        File xmlFolder = new File(basePath);

        if (!xmlFolder.exists()) {
            boolean created = xmlFolder.mkdirs();
            if (!created) {
                throw new IOException("Nu s-a putut crea folderul pentru XML: " + basePath);
            }
        }

        File xmlFile = new File(xmlFolder, "plata_" + System.currentTimeMillis() + ".xml");
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }

        // 🔸 Salvăm în baza de date
        Plata p = new Plata();
        p.setEmail(email);
        p.setTelefon(telefon);
        p.setInmatriculare(inmatriculare);
        p.setDurata(durata);
        p.setSuma(suma);
        p.setTva(tva);
        p.setTotal(total);
        p.setReducere(reducere);
        p.setCodReducere(codReducere);
        p.setXmlContent(xmlContent);
        repo.save(p);

        // === Generăm PDF ===
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(pdf, out);
        pdf.open();

        // Fonturi personalizate (FĂRĂ DIACRITICE)
        Font titlu = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(218, 165, 32)); // gold
        Font subtitlu = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
        Font textNormal = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
        Font bold = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 102, 204));

        // Antet
        Paragraph header = new Paragraph("Ordin de Plată - eGuvernare 2025", titlu);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(20);
        pdf.add(header);

        // Date utilizator
        pdf.add(new Paragraph("Email: " + email, textNormal));
        pdf.add(new Paragraph("Telefon: " + telefon, textNormal));
        pdf.add(new Paragraph("Număr înmatriculare: " + inmatriculare, textNormal));
        pdf.add(new Paragraph("Durata: " + durata + " ore", textNormal));
        pdf.add(new Paragraph(" "));
        LineSeparator linie = new LineSeparator();
        linie.setLineColor(BaseColor.LIGHT_GRAY);
        pdf.add(linie);
        pdf.add(new Paragraph(" "));

        // Detalii plată
        pdf.add(new Paragraph("Detalii plată", subtitlu));
        pdf.add(new Paragraph(String.format("Suma fără TVA: %.2f RON", suma), textNormal));
        pdf.add(new Paragraph(String.format("TVA (19%%): %.2f RON", tva), textNormal));
        pdf.add(new Paragraph(String.format("Total de plată: %.2f RON", suma + tva), bold));

        if (reducere > 0) {
            pdf.add(new Paragraph(" "));
            pdf.add(new Paragraph("Reducere aplicată", subtitlu));
            pdf.add(new Paragraph(String.format("Cod: %s (%.0f%% reducere)", codReducere, reducere), textNormal));
            pdf.add(new Paragraph(String.format("Total cu reducere: %.2f RON", total), totalFont));
        }

        pdf.add(new Paragraph(" "));
        pdf.add(linie);
        Paragraph footer = new Paragraph(
                "Document generat automat - eGuvernare 2025\nDatele sunt protejate conform GDPR.",
                new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        pdf.add(footer);
        pdf.close();

        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(out.toByteArray()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ordin_plata.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }



    @GetMapping("/cupon/{cod}")
    public ResponseEntity<?> verificaCupon(@PathVariable String cod) {
        Optional<Cupon> cuponOpt = cuponRepo.findByCodIgnoreCase(cod);

        if (cuponOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "procent", 0,
                    "mesaj", "❌ Codul introdus nu există."
            ));
        }

        Cupon cupon = cuponOpt.get();

        // Verificăm dacă este activ și în perioada de valabilitate
        var now = java.time.LocalDateTime.now();
        if (!cupon.isActiv() || now.isBefore(cupon.getStartTime()) || now.isAfter(cupon.getEndTime())) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "procent", 0,
                    "mesaj", "⌛ Cuponul a expirat sau nu este activ."
            ));
        }

        // Dacă este valid
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "procent", cupon.getProcent(),
                "mesaj", "✅ Cupon valid - reducere " + cupon.getProcent() + "%"
        ));
    }


    @GetMapping("/raport/analiza")
    public ResponseEntity<InputStreamResource> generateReport() {

        // 1. Extrage toate plățile (înregistrările) din baza de date
        List<Plata> plati = repo.findAll();
        int totalPlati = plati.size();

        // 2. Analiză Date - Tipul 1: Durata (Câmpul 'durata')
        long plati24h = plati.stream().filter(p -> p.getDurata() == 24).count();
        double pondere24h = totalPlati > 0 ? (double) plati24h / totalPlati * 100 : 0.0;

        // Calcul suplimentar pentru Bar Chart Durată (Restul)
        long platiRestul = totalPlati - plati24h;
        double pondereRestul = 100.0 - pondere24h;


        // 2. Analiză Date - Tipul 2: Reducerea Medie (Câmpul 'reducere')
        double sumaReduceri = plati.stream().mapToDouble(Plata::getReducere).sum();
        double reducereMedie = totalPlati > 0 ? sumaReduceri / totalPlati : 0.0;

        // Analiză Suplimentară: Număr de plăți cu reducere
        long platiCuReducere = plati.stream().filter(p -> p.getReducere() > 0).count();
        long platiFaraReducere = totalPlati - platiCuReducere;

        // Calcule procentuale pentru Bar Chart
        double pondereCuReducere = totalPlati > 0 ? (double) platiCuReducere / totalPlati * 100 : 0.0;
        double pondereFaraReducere = 100.0 - pondereCuReducere;


        // 3. Generarea Documentului PDF (Folosind iText)
        PdfWriter writer = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30); // A4 Landscape
            writer = PdfWriter.getInstance(document, out);
            document.open();

            // ==========================================================
            // FONTURI ȘI CONFIGURAȚII
            // ==========================================================
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.NOT_EMBEDDED);

            Font titluFont = new Font(bf, 20, Font.BOLD, BaseColor.BLACK);
            Font sectiuneFont = new Font(bf, 14, Font.BOLD, new BaseColor(218, 165, 32)); // Gold
            Font textNormal = new Font(bf, 12, Font.NORMAL, BaseColor.DARK_GRAY);
            Font textBold = new Font(bf, 12, Font.BOLD, BaseColor.BLACK);
            Font legendFont = new Font(bf, 10, Font.NORMAL, BaseColor.BLACK);
            // ==========================================================


            // TITLU
            Paragraph titlu = new Paragraph("RAPORT DE ANALIZĂ STATISTICĂ: PLĂTI PARCARE", titluFont);
            titlu.setAlignment(Element.ALIGN_CENTER);
            titlu.setSpacingAfter(30);
            document.add(titlu);

            // LINIE DE SEPARARE
            LineSeparator linie = new LineSeparator();
            linie.setLineColor(BaseColor.LIGHT_GRAY);

            // STATISTICI GENERALE
            Paragraph general = new Paragraph("STATISTICI GENERALE", sectiuneFont);
            general.setSpacingAfter(10);
            document.add(general);
            document.add(new Paragraph("Total plăti analizate în baza de date: " + totalPlati + " înregistrări.", textNormal));
            document.add(new Paragraph("\n"));
            document.add(linie);
            document.add(new Paragraph("\n"));

            // ANALIZA 1: DURATA PARCĂRII (Câmpul 'durata')
            Paragraph analiza1 = new Paragraph("ANALIZĂ 1: DISTRIBUȚIA DURATEI (Ponderea plătilor de 24h)", sectiuneFont);
            analiza1.setSpacingAfter(10);
            document.add(analiza1);
            document.add(new Paragraph(String.format("— Număr plăti pentru durata maximă (24h): %d", plati24h), textNormal));
            document.add(new Paragraph(String.format("— Ponderea plătilor de 24h (fată de total): %.2f%%", pondere24h), textNormal));
            document.add(new Paragraph("\n"));

            // ==========================================================
            // DIAGRAMA CU BARE (BAR CHART) PENTRU DURATĂ - SUB ANALIZĂ 1
            // ==========================================================
            Paragraph graficTitlu1 = new Paragraph("REPREZENTARE GRAFICĂ DURATA", sectiuneFont);
            graficTitlu1.setSpacingAfter(10);
            document.add(graficTitlu1);

            PdfContentByte cb = writer.getDirectContent();
            float page_width = document.getPageSize().getWidth(); // Lățimea paginii

            float barWidth = 100;
            float padding = 50; // Spațiul dintre bare
            float total_chart_width = (barWidth * 2) + padding;

            // Calculăm chartX1 pentru a centra Bar Chart-ul
            float chartX1 = (page_width - document.leftMargin() - document.rightMargin() - total_chart_width) / 2 + document.leftMargin();
            float chartY1 = writer.getVerticalPosition(false) - 100; // Poziție Y bazată pe ultima poziție a textului + offset

            // Scalare dinamică a înălțimii barei: Raportăm la numărul maxim de plăti (totalPlati)
            float maxPlati1 = Math.max(plati24h, platiRestul);
            float maxHeight1 = 70.0f;
            float barHeightScale1 = maxPlati1 > 0 ? maxHeight1 / maxPlati1 : 0;

            // Bară 1: plăti 24h
            float bar1Height1 = plati24h * barHeightScale1;
            cb.setColorFill(new BaseColor(0, 102, 204)); // Albastru
            cb.rectangle(chartX1, chartY1, barWidth, bar1Height1);
            cb.fill();

            // Etichetă Bară 1 (24h)
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("24h (%d)".formatted(plati24h), textNormal),
                    chartX1 + (barWidth / 2), chartY1 - 15, 0);
            // Valoare Bară 1
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.format("%.2f%%", pondere24h), textBold),
                    chartX1 + (barWidth / 2), chartY1 + bar1Height1 + 5, 0);


            // Bară 2: Restul
            float bar2X1 = chartX1 + barWidth + padding; // Spațiul dintre bare
            float bar2Height1 = platiRestul * barHeightScale1;
            cb.setColorFill(new BaseColor(192, 192, 192)); // Gri deschis
            cb.rectangle(bar2X1, chartY1, barWidth, bar2Height1);
            cb.fill();

            // Etichetă Bară 2 (Restul)
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Restul (%d)".formatted(platiRestul), textNormal),
                    bar2X1 + (barWidth / 2), chartY1 - 15, 0);
            // Valoare Bară 2
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.format("%.2f%%", pondereRestul), textBold),
                    bar2X1 + (barWidth / 2), chartY1 + bar2Height1 + 5, 0);

            // Linia de bază (axa X)
            cb.setColorStroke(BaseColor.BLACK);
            cb.setLineWidth(1.0f);
            cb.moveTo(chartX1 - 10, chartY1);
            cb.lineTo(bar2X1 + barWidth + 10, chartY1);
            cb.stroke();

            // Spațiere forțată pentru a trece peste diagrama desenată
            float requiredSpace1 = maxHeight1 + 50;
            document.add(new Paragraph("\n"));
            for(int i = 0; i < (requiredSpace1 / 12); i++) {
                document.add(new Paragraph(" "));
            }
            // ==========================================================

            document.add(linie);
            document.add(new Paragraph("\n"));

            // ANALIZA 2: REDUCEREA APLICATĂ (Câmpul 'reducere')
            Paragraph analiza2 = new Paragraph("ANALIZĂ 2: UTILIZAREA CUPONILOR DE REDUCERE", sectiuneFont);
            analiza2.setSpacingAfter(10);
            document.add(analiza2);
            document.add(new Paragraph(String.format("— Număr de plăti la care s-a aplicat reducere: %d", platiCuReducere), textNormal));
            document.add(new Paragraph(String.format("— Reducerea medie aplicată: %.2f%%", reducereMedie), textBold));
            document.add(new Paragraph("\n"));


            // ==========================================================
            // DIAGRAMA CU BARE (BAR CHART) PENTRU REDUCERI - SUB ANALIZĂ 2
            // ==========================================================
            Paragraph graficTitlu2 = new Paragraph("REPREZENTARE GRAFICĂ Reduceri", sectiuneFont);
            graficTitlu2.setSpacingAfter(10);
            document.add(graficTitlu2);

            // --- CENTRAREA DIAGRAMEI 2 ---
            float barWidth2 = 100;
            float padding2 = 50;
            float total_chart_width2 = (barWidth2 * 2) + padding2;

            // Calculăm chartX2 pentru a centra Bar Chart-ul
            float chartX2 = (page_width - document.leftMargin() - document.rightMargin() - total_chart_width2) / 2 + document.leftMargin();
            float chartY2 = writer.getVerticalPosition(false) - 100; // Poziție Y bazată pe ultima poziție a textului + offset

            // Scalare dinamică a înălțimii barei
            float maxPlati2 = Math.max(platiCuReducere, platiFaraReducere);
            float maxHeight2 = 70.0f;
            float barHeightScale2 = maxPlati2 > 0 ? maxHeight2 / maxPlati2 : 0;

            // Bară 1: plăti cu Reducere
            float bar1Height2 = platiCuReducere * barHeightScale2;
            cb.setColorFill(new BaseColor(60, 179, 113)); // Verde mentă
            cb.rectangle(chartX2, chartY2, barWidth2, bar1Height2);
            cb.fill();

            // Etichetă Bară 1 (Cu Reducere)
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Cu Reducere (%d)".formatted(platiCuReducere), textNormal),
                    chartX2 + (barWidth2 / 2), chartY2 - 15, 0);
            // Valoare Bară 1
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.format("%.2f%%", pondereCuReducere), textBold),
                    chartX2 + (barWidth2 / 2), chartY2 + bar1Height2 + 5, 0);


            // Bară 2: plăti Fără Reducere
            float bar2X2 = chartX2 + barWidth2 + padding2; // Spațiu între bare
            float bar2Height2 = platiFaraReducere * barHeightScale2;
            cb.setColorFill(new BaseColor(255, 165, 0)); // Portocaliu
            cb.rectangle(bar2X2, chartY2, barWidth2, bar2Height2);
            cb.fill();

            // Etichetă Bară 2 (Fără Reducere)
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Fără Reducere (%d)".formatted(platiFaraReducere), textNormal),
                    bar2X2 + (barWidth2 / 2), chartY2 - 15, 0);
            // Valoare Bară 2
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.format("%.2f%%", pondereFaraReducere), textBold),
                    bar2X2 + (barWidth2 / 2), chartY2 + bar2Height2 + 5, 0);

            // Linia de bază (axa X)
            cb.setColorStroke(BaseColor.BLACK);
            cb.setLineWidth(1.0f);
            cb.moveTo(chartX2 - 10, chartY2);
            cb.lineTo(bar2X2 + barWidth2 + 10, chartY2);
            cb.stroke();

            // Mutăm conținutul următor sub grafic
            float requiredSpace2 = maxHeight2 + 50;
            document.add(new Paragraph("\n"));
            for(int i = 0; i < (requiredSpace2 / 12); i++) {
                document.add(new Paragraph(" "));
            }
            // ==========================================================


            document.add(linie);
            document.add(new Paragraph("\n"));


            document.add(new Paragraph("\n"));
            document.add(linie);
            document.close();

            // 4. Returnarea Documentului către Browser
            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(out.toByteArray()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=raport_analiza_egov.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (DocumentException e) {
            System.err.println("Eroare la generarea documentului PDF: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Eroare I/O: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.internalServerError().build();
    }


}