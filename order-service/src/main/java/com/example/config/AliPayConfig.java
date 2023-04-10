package com.example.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;

public class AliPayConfig {

    /**
     * 网关地址 TODO
     */
    public static final String PAY_GATEWAY = "https://openapi.alipaydev.com/gateway.do";

    /**
     * 应用id TODO
     */
    public static final String APPID = "2021000119639674";

    /**
     * 应用私钥 TODO
     */
    public static final String APP_PRI_KEY = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDPFLLLz5xQz4dNHqQavHgGSKYiqXOo6ECklWNOAw6Ygz4UDEzsbQG+iBAgQDfKy7r4/a+wcsj3trv5Pv0+TWHYBoFhIOLJ4td0ncM8hVO1Cp+JDTOX3HtYLM7q3jNih3L16txjlYJOLGBVtscNQX7Ziw8+tWPvr1zCjgMR5+XmGlFTLkMBn+bwz66c4qnUCa1ONRSZVUk1xxZJWYfuXUMnQ6biityJkqZbFcljEZLfZ7R888Z7jNmZ+XDHqkuPJymC5y5jk0rw0n+06Fupv3y+kj8KwIUf8pTdViNKprNpf8wNwcTpWaRG8CwKv0S92qRLZXZuDZIn78ua5FT/0ZajAgMBAAECggEADfQXGCSlI8zYERo/dI2+iK3cg/lnEbqUoIJwbpFGKoCtbixmAohV1cDVVVb+a2vO7XgqnnkdkZN+lwzng4szTJsjnKfaoD/oxLLctPdG7KwKk/GPyGvS494hEzJlw/3cgTWSJCOu5BWZaC0ovHvtUDtOi6stULlARtvK2SoHE7oTOTc1YGeHWPScGM6RitbaGRSSabPuCn32NPzc1hK2wBvvk7vdpfTdVYz12yk9g+X5e1vZrggW01XTifdaMDVH3ypkdaslHdMx0uPyaysEgSG7XC5qCeweGvc63+/XXxq1HHi61o2A8VmCennG5EsWazCurGGY0GktWMKQsY48AQKBgQDnfobd9W1YeEwdX7rl9p9oqTPXyM0XQ0JoGWY5aGAuRiv31qdzBgbHwG+PREHjhuvV7Bz9lCYPdnnmkDYBIQoDq6mDkOkv/W9hWu9z5c/2Sk6ouvHfYEOzd/McZwYC3ZZx0fMHTGz8RRQyYmi3LGm7OP2MC+hBOjZeN8LdNbhR+QKBgQDlAJIkmzesH7RvEd7RO4mEpgJtJX9l+xkLso4Ze8/RAWaK5ES5LSPLcxiUjFAcCINxCgmB1ppzNyupw87yza5Nd5LKWwkEK3wuscDyBQMFv/XgorgGqCNI3pWz7EgmZ536R/2fC77FwUDa0V3k3k/TX+hFc8DXThKZy/KLB2DUewKBgQCknFo2tvCcOl6VfJ8gKzDLcZYF2SnNYuxzrav9InVSMl/Ninvj1OM5Y2Nw0q6vph9JLO6oepJd6HdiNiXQw7elSInvnjnTADoVVl5zYXVxwwEQBm870e4STjc3eSLTItJ5+TsBc50D/fMQqN2hcxl0ImBZF5Z923ERqkEMn8iDsQKBgQC7qlMZcqYWBUlteDycROk2boEwhvk3TqZAwsvWsHMm+bnZ+qUjB4387U5odfA0ePmWij4uS/r4jm7WyaNvsQS1cVc3Q5FI/IXhEkRnUZoffSd3NoIO2n1H+zi9YNXjuyhocdSzIQHut6d5avhCVbkfJMqJGAohp1Zw+QTD0IEWvwKBgQCatzJoBEn2Xu/RJdaEOQC3OXChkE4J9RZUaqLmGbPtFP+AqdQJQxac6yM2jGTS9CR1rdBvo/2u+zd0EM31Rt3FfslsYg6HZCjAzqdnO6W9JFOQ33TLJj2SdaRJ7Z4ap+z7lXg8UpAAGcCKPIRt7OmdnEwc3lurlQJ0QPUKT0tFyg==";

    /**
     * 支付宝公钥 TODO
     */
    public static final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjtcOcugLxPDfPTUzzpgq+2g4YrDZUxYsKRJd18/qkFkvVDIj16X0Y7p+fhRSBkgkor1ipt0r3vCHiLSz4iGHuy3kyYgg0PsJkCbwRPgJThxyXGeXHoaJWqCj5LIsdvSBtgB/VrsTCPKnP66MZd60nEkXH7+1zDnsqn4upQbDrobHc1ObF6DLs8r99gEG8esN8SPXQ/LDAaoa+FFx/OL3BaEYcRd22I8FFKLaI+wGkTSDzH8Z8cyVsxT8Q6kRAcgUBK6YfMF4EqCXyWzEuzQ9hi64UMi2DLDN/mQCDwLXyui8fQ1aCelarfpjDEJLflE4dca4zeMOk82NkdrbSYKZoQIDAQAB";

    /**
     * 签名类型
     */
    public static final String SIGN_TYPE = "RSA2";

    /**
     * 字符编码
     */
    public static final String CHARSET = "UTF-8";

    /**
     * 返回参数格式
     */
    public static final String FORMART = "json";

    private AliPayConfig(){

    }

    private volatile static AlipayClient instance = new DefaultAlipayClient(PAY_GATEWAY,APPID,APP_PRI_KEY,FORMART,CHARSET,ALIPAY_PUBLIC_KEY,SIGN_TYPE);

    public static AlipayClient getInstance(){//直接饿汉式加载 或者双重判断 + 锁校验
        return instance;
    }

}
