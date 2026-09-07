DROP TABLE ozlist CASCADE;
CREATE TABLE ozlist
(
   id                  int             NOT NULL,
   ChapterNo           varchar(20),
   TypeSection         varchar(60),
   TypeFigure          varchar(20),
   Figure_Num          varchar(100),
   Final_Pg_No         varchar(100),
   page_pos            varchar(50),
   Pickup_PrevTextRef  varchar(2000),
   P_Num               varchar(20),
   Description         varchar(1000),
   `Kill`              varchar(20),
   Color               varchar(200),
   BW                  varchar(20),
   Pict_No             varchar(200),
   DesignSize          varchar(50),
   PhotoSize           varchar(50),
   SourcePhotog        varchar(500),
   Credit              varchar(2000),
   Camera_Copy         varchar(200),
   Free                varchar(20),
   RoyaltyFree         varchar(60),
   ObtainedbyAuthor    varchar(20),
   ChapOpener          varchar(200),
   WorkforHire         varchar(60),
   ModelRelease        varchar(20),
   New                 varchar(200),
   Retain              varchar(200),
   Reuse               varchar(20),
   Archive             varchar(100),
   prodNotes           varchar(1000),
   deptNotes           varchar(5000),
   Company             varchar(200),
   SourceId            varchar(200),
   ISBN                varchar(20),
   status              varchar(20),
   error               varchar(1000),
   project_number      varchar(20),
   po_number           varchar(20),
   perm_req_status     varchar(2),
   request_type        varchar(2),
   asset_use_id        varchar(20),
   estimated_price     varchar(20),
   price               varchar(20),
   canceled_flag       boolean,
  source_name          varchar(500)
)
ENGINE=InnoDB;

ALTER TABLE ozlist
   ADD CONSTRAINT pk_ozlist
   PRIMARY KEY (id);
