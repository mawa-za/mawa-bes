package za.co.mawa.bes.dto;

import java.io.Serializable;

public class AttachmentDto implements Serializable {
   private String id;

   public void setId(String id)
   {
       this.id = id;
   }

   public String getId()
   {
       return id;
   }

   private String uploadDate;
   public void setUploadDate(String uploadDate)
   {
      this.uploadDate = uploadDate;
   }

   public String getUploadDate()
   {
      return uploadDate;

   }

   private String uploadTime;
   public void setUploadTime(String uploadTime)
   {
      this.uploadTime = uploadTime;
   }

   public String getUploadTime()
   {
      return uploadTime;
   }

   private String uploadedBy;

   public void setUploadedBy(String uploadedBy)
   {
      this.uploadedBy = uploadedBy;
   }

   public String getUploadedBy()
   {
      return uploadedBy;
   }

   private String downloadDate;

   public void setDownloadDate(String downloadDate)
   {
      this.downloadDate = downloadDate;
   }

   public String getDownloadDate()
   {
      return downloadDate;
   }

   private String downloadedBy;

   public void setDownloadedBy(String downloadedBy)
   {
      this.downloadedBy = downloadedBy;
   }

   public String getDownloadedBy()
   {
      return downloadedBy;
   }

   private String file;

   public void setFile(String file)
   {
      this.file = file;
   }

   public String getFile()
   {
      return file;
   }
}
