package com.egov2025.controller;

import com.egov2025.model.Cupon;
import com.egov2025.model.Plata;
import com.egov2025.repository.CuponRepository;
import com.egov2025.repository.PlataRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
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

        // Fonturi personalizate
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

}
