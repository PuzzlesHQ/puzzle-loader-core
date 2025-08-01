import subprocess

def run(task_name = None, flags = None):
    args = ["gradle"]
    if type(task_name) is str:
        args.append(task_name)
    if type(task_name) is list:
        for x in task_name:
            args.append(x)

    if type(flags) is str:
        args.append(task_name)
    if type(flags) is list:
        for x in flags:
            args.append("--" + x)

    subprocess.call(args=args)
