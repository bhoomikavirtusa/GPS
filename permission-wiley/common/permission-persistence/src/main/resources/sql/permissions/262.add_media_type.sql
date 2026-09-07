-- merged with seed

insert into media_type (code, description, sort_order) values ('Graph', 'Graph', 9);

-- reorder
update media_type set sort_order = 1 where code = 'Animation';
update media_type set sort_order = 2 where code = 'Article';
update media_type set sort_order = 3 where code = 'Audio';
update media_type set sort_order = 4 where code = 'Cartoon';
update media_type set sort_order = 5 where code = 'Clip Art';
update media_type set sort_order = 6 where code = 'CoverDesign';
update media_type set sort_order = 7 where code = 'DataSet';
update media_type set sort_order = 8 where code = 'Extract';
update media_type set sort_order = 9 where code = 'Graph';
update media_type set sort_order = 10 where code = 'Icon';
update media_type set sort_order = 11 where code = 'Illustration';
update media_type set sort_order = 12 where code = 'Interactivity';
update media_type set sort_order = 13 where code = 'LecturePresentation';
update media_type set sort_order = 14 where code = 'List';
update media_type set sort_order = 15 where code = 'Map';
update media_type set sort_order = 16 where code = 'Photo';
update media_type set sort_order = 17 where code = 'Question';
update media_type set sort_order = 18 where code = 'Realia';
update media_type set sort_order = 19 where code = 'Screenshot';
update media_type set sort_order = 20 where code = 'Table';
update media_type set sort_order = 21 where code = 'Text';
update media_type set sort_order = 22 where code = 'Tutorial';
update media_type set sort_order = 23 where code = 'Video';
