-- Project documentation catalogue used by the web admin deliverables page.
CREATE TABLE IF NOT EXISTS project_deliverables (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    title       VARCHAR(180) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category    VARCHAR(80)  NOT NULL DEFAULT 'Project documentation',
    html_file   VARCHAR(180) NOT NULL,
    pdf_file    VARCHAR(180) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
