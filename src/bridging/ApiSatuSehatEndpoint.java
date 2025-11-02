/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bridging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import fungsi.koneksiDB;
import fungsi.sekuel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneOffset;
import java.util.Date;
import javax.swing.JOptionPane;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 *
 * @author fathurrahman
 */
public class ApiSatuSehatEndpoint {
    
    private Connection koneksi=koneksiDB.condb();
    private sekuel Sequel=new sekuel();
    private String json="",link="",idrequest="";
    private ApiSatuSehat api=new ApiSatuSehat();
    private HttpHeaders headers;
    private HttpEntity requestEntity;
    private ObjectMapper mapper= new ObjectMapper();
    private JsonNode root;
    private JsonNode response;
    private PreparedStatement ps;
    private ResultSet rs;
    private String[] arrSplit;
    private SimpleDateFormat tanggalFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Date date = new Date();  
    private SatuSehatCekNIK cekViaSatuSehat=new SatuSehatCekNIK();
    
    public ApiSatuSehatEndpoint() {
        try {
            link=koneksiDB.URLFHIRSATUSEHAT();
            
        } catch (Exception e) {
            System.out.println("Notif : "+e);
        }
    }
    
    public String ubahZonaWaktu(String tglReg, String jamReg) {        
        LocalDate tanggal = LocalDate.parse(tglReg);
        
        LocalTime waktu;
        
        try {
            waktu = LocalTime.parse(jamReg);
        } catch(DateTimeParseException e) {
            waktu = LocalTime.parse(jamReg, DateTimeFormatter.ofPattern("HH:mm"));
        }
        
        LocalDateTime localDateTime = LocalDateTime.of(tanggal, waktu);
        
        ZoneId zonaAsli = ZoneId.of("Asia/Makassar");
        
        ZonedDateTime zonedDateTimeAsli = localDateTime.atZone(zonaAsli);
        
        // 5. Konversi ke UTC (+00:00)
        ZonedDateTime zonedDateTimeUtc = zonedDateTimeAsli.withZoneSameInstant(ZoneOffset.UTC);

        // 6. Format ke String ISO yang diminta
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        String hasilString = zonedDateTimeUtc.format(formatter);
        
        return hasilString;
    } 
    
    public void encounter(String noRawat, String kdDokter, String rmPasien, String kdPoli, boolean update) {
        String nikDokter = Sequel.cariIsi("SELECT no_ktp FROM pegawai WHERE nik = ?", kdDokter);
        String nmDokter = Sequel.cariIsi("SELECT nama FROM pegawai WHERE nik = ?", kdDokter);
        
        String nikPasien = Sequel.cariIsi("SELECT no_ktp FROM pasien WHERE no_rkm_medis = ?", rmPasien);
        String nmPasien = Sequel.cariIsi("SELECT nm_pasien FROM pasien WHERE no_rkm_medis = ?", rmPasien); 
        
        String tglReg = Sequel.cariIsi("SELECT tgl_registrasi FROM reg_periksa WHERE no_rawat = ?", noRawat);
        String jamReg = Sequel.cariIsi("SELECT jam_reg FROM reg_periksa WHERE no_rawat = ?", noRawat);
        String sttsLanjut = Sequel.cariIsi("SELECT status_lanjut FROM reg_periksa WHERE no_rawat = ?", noRawat);
        
        String kdLocation = Sequel.cariIsi("SELECT id_lokasi_satusehat FROM satu_sehat_mapping_lokasi_ralan WHERE kd_poli = ?", kdPoli);
        String nmPoli = Sequel.cariIsi("SELECT nm_poli FROM poliklinik WHERE kd_poli = ?", kdPoli);
        
        String actCode = "", actCodeDisplay="";

        
        if(sttsLanjut.equals("Ralan")) {
            if(kdPoli.equals("IGDK")) {
                actCode = "EMER";
                actCodeDisplay = "emergency";
            }
            else {
                actCode = "AMB";
                actCodeDisplay = "ambulatory";
            }  
        }
          
        try {
            String idDokter=cekViaSatuSehat.tampilIDParktisi(nikDokter);
            String idPasien=cekViaSatuSehat.tampilIDPasien(nikPasien);
            
            String time = ubahZonaWaktu(tglReg, jamReg);
            
            if (time != null && time.endsWith("Z")) {
                // Hapus 'Z' di akhir dan tambahkan "+00:00"
                time = time.substring(0, time.length() - 1) + "+00:00";
            }

            if((!idDokter.equals("")) && (!idPasien.equals(""))) {
                try {
                    headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add("Authorization", "Bearer "+api.TokenSatuSehat());

                    json = "{" +
                                "\"resourceType\": \"Encounter\"," +
                                "\"identifier\": [" +
                                    "{" +
                                        "\"system\": \"http://sys-ids.kemkes.go.id/encounter/"+koneksiDB.IDSATUSEHAT()+"\"," +
                                        "\"value\": \""+noRawat+"\"" +
                                    "}" +
                                "]," +
                                "\"status\": \"arrived\"," +
                                "\"class\": {" +
                                    "\"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\"," +
                                    "\"code\": \""+actCode+"\"," +
                                    "\"display\": \""+actCodeDisplay+"\"" +
                                "}," +
                                "\"subject\": {" +
                                    "\"reference\": \"Patient/"+idPasien+"\"," +
                                    "\"display\": \""+nmPasien+"\"" +
                                "}," +
                                "\"participant\": [" +
                                    "{" +
                                        "\"type\": [" +
                                            "{" +
                                                "\"coding\": [" +
                                                    "{" +
                                                        "\"system\": \"http://terminology.hl7.org/CodeSystem/v3-ParticipationType\"," +
                                                        "\"code\": \"ATND\"," +
                                                        "\"display\": \"attender\"" +
                                                    "}" +
                                                "]" +
                                            "}" +
                                        "]," +
                                        "\"individual\": {" +
                                            "\"reference\": \"Practitioner/"+idDokter+"\"," +
                                            "\"display\": \""+nmDokter+"\"" +
                                        "}" +
                                    "}" +
                                "]," +
                                "\"period\": {" +
                                    "\"start\": \""+time+"\"" +
                                "}," +
                                "\"location\": [" +
                                    "{" +
                                        "\"location\": {" +
                                            "\"reference\": \"Location/"+kdLocation+"\"," +
                                            "\"display\": \""+nmPoli+"\"" +
                                        "}" +
                                    "}" +
                                "]," +
                                "\"statusHistory\": [" +
                                    "{" +
                                        "\"status\": \"arrived\"," +
                                        "\"period\": {" +
                                            "\"start\": \""+time+"\"" +
                                        "}" +
                                    "}" +
                                "]," +
                                "\"serviceProvider\": {" +
                                    "\"reference\": \"Organization/"+koneksiDB.IDSATUSEHAT()+"\"" +
                                "}" +
                            "}";
                    
                    requestEntity = new HttpEntity(json,headers);
                    System.out.println("payload: "+json);
                    json=api.getRest().exchange(link+"/Encounter", HttpMethod.POST, requestEntity, String.class).getBody();
                    root = mapper.readTree(json);
                    response = root.path("id");
                    
                    if(!response.asText().equals("")){
                        if(!update) {
                            Sequel.menyimpan2("satu_sehat_encounter","?,?","No.Rawat",2,new String[]{
                                noRawat,response.asText()
                            });
                            
//                            JOptionPane.showMessageDialog(null, "Pengiriman Kunjungan ke Satu Sehat Berhasil");
                            System.out.println("Berhasil : \n" + json);
                        } else {
                            Sequel.mengedit("satu_sehat_encounter", "id_encounter="+response.asText(), "no_rawat="+noRawat);
                            
                            System.out.println("Berhasil : \n" + json);
//                            JOptionPane.showMessageDialog(null, "Update Kunjungan ke Satu Sehat Berhasil");

                        }
                        
                    }
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    // Handle client and server errors
                    System.err.println("Error Response Status Code: " + e.getStatusCode());

                    // You can further parse the error response body if needed
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode errorResponse = mapper.readTree(e.getResponseBodyAsString());
                    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
                    String prettyErrorResponse = writer.writeValueAsString(errorResponse);
                    System.err.println("Error Response JSON: \n" + prettyErrorResponse);
                    
                    JOptionPane.showMessageDialog(null, "Error Kirim Data Encounter. Pesan Error:\n" + prettyErrorResponse);
                    
                }
            }
        } catch(Exception e) {
            System.out.println("Notifikasi : "+e);
            
        }
        
    }
    
    
}
