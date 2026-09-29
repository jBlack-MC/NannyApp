"""Read-only diagnostics for the latest GitHub Actions run; requires authenticated gh."""
import json
import subprocess
import sys


def gh(*args):
    result = subprocess.run(["gh", *args], capture_output=True, text=True, check=False)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or "GitHub CLI request failed")
    return json.loads(result.stdout)


def main():
    repo = gh("repo", "view", "--json", "nameWithOwner")["nameWithOwner"]
    runs = gh("api", f"repos/{repo}/actions/runs?per_page=1")["workflow_runs"]
    if not runs:
        print("No Actions runs found. Commit/push workflows and check repository Actions settings.")
        return 2
    run = runs[0]
    print(f"{run['name']}: {run['status']} / {run['conclusion'] or 'pending'}")
    print(run["html_url"])
    print(f"Revision: {run['head_sha']}")
    jobs = gh("api", "--paginate", "--slurp", f"repos/{repo}/actions/runs/{run['id']}/jobs?per_page=100")
    for page in jobs:
        for job in page["jobs"]:
            print(f"\n{job['name']}: {job['conclusion'] or job['status']}")
            if not job["steps"]:
                print("  No steps executed; inspect runner/account/configuration errors below.")
            for step in job["steps"]:
                if step["conclusion"] == "failure":
                    print(f"  Failed step: {step['name']}")
            annotations = gh("api", "--paginate", "--slurp", job["check_run_url"] + "/annotations?per_page=100")
            for batch in annotations:
                for item in batch:
                    print(f"  {item['annotation_level']}: {item['message']}")
    if run["status"] != "completed":
        return 2
    return 0 if run["conclusion"] == "success" else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, RuntimeError, ValueError, KeyError) as error:
        print(f"Unable to inspect Actions: {error}", file=sys.stderr)
        sys.exit(2)
