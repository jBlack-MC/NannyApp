<?php
require_once __DIR__ . '/../config/config.php';
require_role('admin');

$catalogue = [
    ['slug' => 'product-requirements', 'title' => 'Product requirements', 'description' => 'Project scope, users, features and assessment requirements.', 'html' => '01-product-requirements.html', 'pdf' => '01-product-requirements.pdf'],
    ['slug' => 'technical-requirements', 'title' => 'Technical requirements', 'description' => 'Technology stack, security, database and deployment requirements.', 'html' => '02-technical-requirements.html', 'pdf' => '02-technical-requirements.pdf'],
    ['slug' => 'app-flow', 'title' => 'Application flow', 'description' => 'User journeys and the web-to-mobile application flow.', 'html' => '03-app-flow.html', 'pdf' => '03-app-flow.pdf'],
    ['slug' => 'design-brief', 'title' => 'Design brief', 'description' => 'The interface direction, visual language and accessibility choices.', 'html' => '04-design-brief.html', 'pdf' => '04-design-brief.pdf'],
    ['slug' => 'backend-schema', 'title' => 'Backend schema and API', 'description' => 'Database tables, API responsibilities and authentication architecture.', 'html' => '05-backend-schema.html', 'pdf' => '05-backend-schema.pdf'],
    ['slug' => 'implementation-plan', 'title' => 'Implementation plan', 'description' => 'Implementation phases, testing, DevOps and delivery evidence.', 'html' => '06-implementation-plan.html', 'pdf' => '06-implementation-plan.pdf'],
    ['slug' => 'ci-cd-and-release', 'title' => 'CI/CD and APK release', 'description' => 'GitHub Actions, signed APK publishing, web distribution and release evidence.', 'html' => '07-ci-cd-and-release.html', 'pdf' => '07-ci-cd-and-release.pdf'],
];

// Catalogue metadata is persisted when the migration is available. The
// static allow-list remains the authority for files and is also a safe
// fallback while an older deployment is waiting for the migration.
try {
    $driver = defined('DB_DRIVER') ? DB_DRIVER : 'mysql';
    $sql = $driver === 'pgsql'
        ? 'INSERT INTO project_deliverables (slug,title,description,category,html_file,pdf_file,sort_order,is_active) VALUES (?,?,?,?,?,?,?,1) ON CONFLICT (slug) DO UPDATE SET title=EXCLUDED.title, description=EXCLUDED.description, html_file=EXCLUDED.html_file, pdf_file=EXCLUDED.pdf_file, sort_order=EXCLUDED.sort_order, is_active=1'
        : 'INSERT INTO project_deliverables (slug,title,description,category,html_file,pdf_file,sort_order,is_active) VALUES (?,?,?,?,?,?,?,1) ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), html_file=VALUES(html_file), pdf_file=VALUES(pdf_file), sort_order=VALUES(sort_order), is_active=1';
    $stmt = db()->prepare($sql);
    foreach ($catalogue as $i => $item) {
        $stmt->execute([$item['slug'], $item['title'], $item['description'], 'Project documentation', $item['html'], $item['pdf'], $i + 1]);
    }
} catch (Throwable $e) {
    // A missing migration must not prevent an administrator from accessing
    // the checked-in documents. The next migration run will create the table.
}

$pageTitle = 'Project deliverables';
require __DIR__ . '/../includes/header.php';
?>
<main class="page-shell admin-page">
    <div class="page-heading admin-heading">
        <div>
            <p class="h-eyebrow">Project documentation</p>
            <h1>Task 2 deliverables</h1>
            <p class="muted">Architecture, requirements, design and implementation evidence for the Nanny-App prototype.</p>
        </div>
        <a class="btn" href="<?= url('admin/dashboard.php') ?>">← Admin dashboard</a>
    </div>

    <section class="grid grid-2" aria-label="Project deliverables">
        <?php foreach ($catalogue as $item): ?>
            <article class="card admin-action-card">
                <span class="pill">Project documentation</span>
                <h2><?= e($item['title']) ?></h2>
                <p class="muted"><?= e($item['description']) ?></p>
                <div class="button-row mt-4">
                    <a class="btn btn-primary" target="_blank" rel="noopener" href="<?= url('admin/deliverable-file.php?slug=' . rawurlencode($item['slug']) . '&format=html') ?>">Open HTML</a>
                    <a class="btn" download href="<?= url('admin/deliverable-file.php?slug=' . rawurlencode($item['slug']) . '&format=pdf') ?>">Download PDF</a>
                </div>
            </article>
        <?php endforeach; ?>
    </section>
</main>
<?php require __DIR__ . '/../includes/footer.php'; ?>
