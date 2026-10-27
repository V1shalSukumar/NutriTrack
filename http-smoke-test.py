"""Integration check against a running LOCAL demo server. Adds and removes one test meal."""
import urllib.request, urllib.parse, urllib.error, http.cookiejar, re, json, sys
BASE = sys.argv[1] if len(sys.argv)>1 else 'http://127.0.0.1:8080'
results=[]
def client():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def request(c,path,data=None,headers=None):
    if isinstance(data,dict): data=urllib.parse.urlencode(data).encode()
    try:
        with c.open(urllib.request.Request(BASE+path,data,headers or {}),timeout=20) as r:
            return r.status,r.read().decode(),r.geturl(),dict(r.headers)
    except urllib.error.HTTPError as e:return e.code,e.read().decode(),e.url,dict(e.headers)
def check(name,condition):
    results.append({'check':name,'passed':bool(condition)})
    if not condition: raise AssertionError(name)
def token(html):return re.search('name="csrf" value="([^"]+)"',html).group(1)
u=client();status,html,url,_=request(u,'/dashboard')
check('Unauthenticated requests redirect to login',url.endswith('/login'))
t=token(html)
check('CSRF rejects missing token',request(u,'/login',{'email':'user@nutritrack.com','password':'user123','role':'USER'})[0]==403)
status,html,_,_=request(u,'/login',{'csrf':t,'email':'user@nutritrack.com','password':'incorrect','role':'USER'})
check('Incorrect password displays custom exception',status==200 and 'Email or password is incorrect.' in html)
status,html,url,_=request(u,'/login',{'csrf':t,'email':'user@nutritrack.com','password':'user123','role':'USER'})
check('User login renders calculated dashboard',status==200 and url.endswith('/dashboard') and 'Gap detector' in html)
check('Removed review page returns 404',request(u,'/review')[0]==404)
check('User cannot open admin page',request(u,'/admin')[0]==403)
for page in ['log','daily','weekly']:
    check(page+' JSP renders',request(u,'/'+page)[0]==200)
status,css,_,headers=request(u,'/assets/app.css')
check('CSS served as stylesheet',status==200 and 'text/css' in headers.get('Content-Type',''))
status,html,_,_=request(u,'/log',{'csrf':t,'action':'add','foodId':'8','quantity':'0','mealType':'Lunch'})
check('Server-side InvalidQuantityException is displayed','Quantity must be greater than 0' in html)
request(u,'/log',{'csrf':t,'action':'add','foodId':'8','quantity':'2','mealType':'Lunch'})
status,html,url,_=request(u,'/log',{'csrf':t,'action':'save'})
check('Meal save commits and redirects to daily log',status==200 and url.endswith('/daily') and 'Meal saved.' in html and 'Toor Dal' in html)
ids=re.findall(r'name="id" value="(\d+)"',html)
test_id=max(map(int,ids))
check('Saved meal can be removed',request(u,'/daily',{'csrf':t,'id':test_id})[0]==200)
status,report,_,headers=request(u,'/report')
check('Report downloads seven days of CSV',status==200 and len(report.strip().splitlines())==8 and 'attachment' in headers.get('Content-Disposition',''))
a=client();_,html,_,_=request(a,'/login');at=token(html)
status,html,url,_=request(a,'/login',{'csrf':at,'email':'admin@nutritrack.com','password':'admin123','role':'ADMIN'})
check('Admin login renders food/user management',status==200 and 'Food catalogue' in html and 'Users' in html)
boundary='NutriTrackTestBoundary'
csv='name,category,calories,protein,carbs,fat,fibre,unit,unit_grams\nSmokeTest Fruit,Fruit,50,1,10,0,2,piece,100\n'
payload=(f'--{boundary}\r\nContent-Disposition: form-data; name="action"\r\n\r\nimport\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="smoke.csv"\r\nContent-Type: text/csv\r\n\r\n{csv}\r\n--{boundary}--\r\n').encode()
status,html,_,_=request(a,'/admin?csrf='+at,payload,{'Content-Type':'multipart/form-data; boundary='+boundary})
check('Multipart CSV import reaches JDBC and refreshes cache',status==200 and '1 foods imported successfully.' in html and 'SmokeTest Fruit' in html)
row=re.search(r'<tr><td>SmokeTest Fruit</td>.*?</tr>',html,re.S).group(0)
fid=re.search(r'name="id" value="(\d+)"',row).group(1)
check('Admin can archive test food',request(a,'/admin',{'csrf':at,'action':'deleteFood','id':fid})[0]==200)
status,html,_,_=request(a,'/admin',{'csrf':at,'action':'job'})
check('Background job can be queued','Background report job queued.' in html)
status,html,url,_=request(u,'/logout',{'csrf':t})
check('Logout ends authenticated session',url.endswith('/login') and request(u,'/dashboard')[2].endswith('/login'))
print(json.dumps({'passed':len(results),'results':results},indent=2))
