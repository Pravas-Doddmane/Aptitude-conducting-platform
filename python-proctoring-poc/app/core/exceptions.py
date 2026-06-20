class ProctoringException(Exception):
    """Base exception for proctoring."""
    pass

class SessionNotInitialized(ProctoringException):
    pass