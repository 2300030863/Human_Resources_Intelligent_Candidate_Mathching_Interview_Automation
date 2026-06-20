from services.ai_service import AIService
import time

ai = AIService()
tests = []
for i in range(3):
    start = time.time()
    q = ai.generate_question('Engineer', 'Python', 'Basic', [])
    elapsed = time.time() - start
    tests.append(elapsed)
    print(f'Test {i+1}: {elapsed:.2f}s')

avg = sum(tests) / len(tests)
print(f'Average generation time: {avg:.2f}s')
