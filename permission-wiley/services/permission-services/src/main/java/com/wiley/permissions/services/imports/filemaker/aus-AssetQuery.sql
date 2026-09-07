select ChapterNo,TypeSection,TypeFigure,Figure_Num,Final_Pg_No,page_pos,Pickup_PrevTextRef,P_Num,Description,
"Kill",Color,BW,Pict_No,DesignSize,PhotoSize,SourcePhotog,Credit,Camera_Copy,Free,RoyaltyFree,ObtainedbyAuthor,
ChapOpener,WorkforHire,ModelRelease,"New",Retain,Reuse,Archive,deptNotes,prodNotes,Company,SourceId,ISBN,id,status,error, asset_use_id, estimated_price, price  from ozList 
where id > 59999  order by id
