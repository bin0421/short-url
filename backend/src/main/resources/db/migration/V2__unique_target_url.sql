-- One Short Link per Target URL (ADR 0001). Indexed by hash because a 2048-character
-- non-ASCII URL can exceed the B-tree entry size limit.
CREATE UNIQUE INDEX short_link_target_url_md5 ON short_link (md5(target_url));
