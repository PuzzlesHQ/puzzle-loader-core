import subprocess

def commit(message, files = None):
    if type(files) is str:
        subprocess.call(args=["git", "commit", "-m", message, "--", files])
        return

    if type(files) is list:
        args = ["git", "commit", "-m", message, "--"]

        for x in files:
            args.append(x)
        subprocess.call(args=args)
        return

    subprocess.call(args=["git", "commit", "-m", message])
    return

def init_credentials(username, email):
    subprocess.call(args=["git", "config", "--local", "user.name", username])
    subprocess.call(args=["git", "config", "--local", "user.email", email])

def add(files = None):
    if type(files) is str:
        subprocess.call(args=["git", "add", files])
        return

    if type(files) is list:
        args = ["git", "add"]

        for x in files:
            args.append(x)
        subprocess.call(args=args)
        return

    subprocess.call(args=["git", "add", "."])

def reset(file = None):
    if not (file is None):
        subprocess.call(args=["git", "reset", file])
        return
    subprocess.call(args=["git", "reset"])

def checkout(branch_name, create_new = False):
    if create_new == "orphan":
        subprocess.call(args=["git", "checkout", "--orphan", branch_name])
        return
    if create_new:
        subprocess.call(args=["git", "checkout", "-b", branch_name])
        return
    subprocess.call(args=["git", "checkout", branch_name])

def add_remote(url, remote = "origin"):
    subprocess.call(args=["git", "remote", "add", remote, url])

def pull(remote = "origin", branch_name = "main"):
    subprocess.call(args=["git", "pull", remote, branch_name])

def push(remote = "origin", branch_name = "main"):
    subprocess.call(args=["git", "push", "-u", remote, branch_name])